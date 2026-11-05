package com.mycompany.chat_cliente_web;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/** Emissor limitado, sem arquivos, sem retry e sem acesso à UI/request nos callbacks. */
public final class RelatorErrosWeb {
    private static final RelatorErrosWeb PADRAO = new RelatorErrosWeb();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1))
            .followRedirects(HttpClient.Redirect.NEVER).build();
    private final Semaphore vagas = new Semaphore(16);
    private final Map<String, Long> repeticoes = new LinkedHashMap<>();
    private final AtomicLong proximoAviso = new AtomicLong();
    private final String computador = System.getProperty("chat.logs.computador",
            "glassfish-executor:" + System.getenv().getOrDefault("COMPUTERNAME", System.getenv().getOrDefault("HOSTNAME", "nao-identificado")));

    public static RelatorErrosWeb padrao() { return PADRAO; }

    public void recuperar(String classe, String metodo, String contexto) {
        synchronized (repeticoes) { repeticoes.remove(chave(classe, metodo, contexto)); }
    }

    /** false significa suprimido/sem capacidade/configuração inválida; nunca lança falha de logging. */
    public boolean registrar(String endereco, String classe, String metodo, String contexto, Exception erro) {
        try {
            String chave = chave(classe, metodo, contexto);
            URI uri = URI.create(endereco);
            if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) || uri.getHost() == null
                    || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                    || !"/erros/clientes".equals(uri.getPath())) throw new IllegalArgumentException();
            String corpo = "versao=1&tipo=web&computador=" + codificar(computador, 128)
                    + "&classe=" + codificar(classe, 256) + "&metodo=" + codificar(metodo, 128)
                    + "&excecao=" + codificar(erro.getClass().getName(), 256)
                    + "&mensagem=" + codificar(erro.getMessage() == null ? "Sem mensagem." : erro.getMessage(), 768);
            HttpRequest.Builder pedido = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(2))
                    .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            String token = System.getProperty("chat.logs.web.token", "");
            if (!token.isEmpty() && contexto != null && !contexto.isBlank()) {
                pedido.header("X-Chat-Logs-Token", token);
                corpo += "&ipOriginal=" + codificar(contexto, 64);
            }
            // Limite inclusive depois do percent-encoding; nunca envia corpo maior que o contrato.
            if (corpo.length() > 16384) throw new IllegalArgumentException();
            HttpRequest requisicao = pedido.POST(HttpRequest.BodyPublishers.ofString(corpo)).build();
            java.util.concurrent.CompletableFuture<HttpResponse<Void>> envio;
            synchronized (repeticoes) {
                long agora = System.nanoTime();
                Long anterior = repeticoes.get(chave);
                if (anterior != null && agora - anterior < TimeUnit.SECONDS.toNanos(30)) return false;
                if (!vagas.tryAcquire()) { avisar(); return false; }
                try {
                    // Apenas inicia a operação assíncrona; não espera a rede sob este bloqueio.
                    envio = http.sendAsync(requisicao, HttpResponse.BodyHandlers.discarding());
                } catch (RuntimeException e) {
                    vagas.release();
                    avisar();
                    return false;
                }
                // Só tentativas efetivamente admitidas iniciam supressão/expulsam chaves antigas.
                // Não há rollback de chave que possa apagar uma admissão concorrente mais recente.
                if (repeticoes.size() >= 128) repeticoes.remove(repeticoes.keySet().iterator().next());
                repeticoes.put(chave, System.nanoTime());
            }
            envio.whenComplete((resposta, falha) -> {
                vagas.release();
                if (falha != null || resposta.statusCode() != 204) avisar();
            });
            return true;
        } catch (RuntimeException e) { avisar(); return false; }
    }

    private static String chave(String classe, String metodo, String contexto) { return classe + ":" + metodo + ":" + contexto; }
    private static String codificar(String texto, int limite) {
        String valor = texto == null ? "-" : texto;
        if (valor.length() > limite) {
            int fim = limite - 11;
            if (Character.isHighSurrogate(valor.charAt(fim - 1))) fim--;
            valor = valor.substring(0, fim) + " [truncado]";
        }
        return URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }
    private void avisar() {
        long agora = System.nanoTime(), antes = proximoAviso.get();
        if (agora >= antes && proximoAviso.compareAndSet(antes, agora + TimeUnit.SECONDS.toNanos(30)))
            System.err.println("Coleta de erros indisponível/limitada; evento sem confirmação. Sem reenvio automático.");
    }
}
