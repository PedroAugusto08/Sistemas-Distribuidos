package pacote;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

/** Serviço independente do histórico e dos dois serviços de mensagens. */
public final class ColetorErrosHttp {
    private ArquivadorErros arquivos;
    private final String bind;
    private final int porta;
    private final String tokenWeb;
    private final Set<String> paresWeb;
    private HttpServer servidor;
    private ExecutorService tarefas;
    private volatile boolean ativo;

    public ColetorErrosHttp() {
        this(null, System.getProperty("chat.logs.bind", "127.0.0.1"),
                Integer.getInteger("chat.logs.port", 8082), System.getProperty("chat.logs.web.token", ""),
                Set.of(System.getProperty("chat.logs.web.peer", "127.0.0.1")));
    }

    public ColetorErrosHttp(ArquivadorErros arquivos, String bind, int porta, String tokenWeb, Set<String> paresWeb) {
        this.arquivos = arquivos;
        this.bind = bind;
        this.porta = porta;
        this.tokenWeb = tokenWeb;
        this.paresWeb = Set.copyOf(paresWeb);
    }

    public synchronized void iniciar() throws IOException {
        if (ativo) return;
        try {
            if (arquivos == null) arquivos = Util.obterLogs();
            // E/S só nos escritores. A ativação não marca sucesso com destino inválido.
            arquivos.verificarCliente("desktop").get(2, TimeUnit.SECONDS);
            arquivos.verificarCliente("web").get(2, TimeUnit.SECONDS);
            Util.configurarHttp();
            servidor = HttpServer.create();
            servidor.bind(new InetSocketAddress(bind, porta), 32);
            // Atendimentos aguardam rede e confirmação do escritor; o limite de admissões permanece separado.
            tarefas = Executors.newVirtualThreadPerTaskExecutor();
            servidor.setExecutor(tarefas);
            Semaphore admissoes = new Semaphore(32); // privado desta geração
            servidor.createContext("/erros/clientes", e -> atender(e, admissoes));
            servidor.start();
            ativo = true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            pararServidor();
            throw new IOException("Ativação interrompida.", e);
        } catch (ExecutionException | TimeoutException | IOException | RuntimeException e) {
            pararServidor();
            Util.registrarErro("internal", "coletor", "-", getClass().getName(), "iniciar", e);
            throw new IOException("Não foi possível iniciar coletor de erros.", e);
        }
    }

    private void atender(HttpExchange exchange, Semaphore admissoes) {
        boolean admitido = admissoes.tryAcquire();
        try (exchange) {
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
            if (!admitido) { responder(exchange, 429); return; }
            if (!exchange.getRequestURI().getRawPath().equals("/erros/clientes")
                    || exchange.getRequestURI().getRawQuery() != null) { responder(exchange, 404); return; }
            if (exchange.getRequestHeaders().containsKey("Origin")) { responder(exchange, 403); return; }
            if (!exchange.getRequestMethod().equals("POST")) {
                exchange.getResponseHeaders().set("Allow", "POST");
                responder(exchange, 405); return;
            }
            String conteudo = exchange.getRequestHeaders().getFirst("Content-Type");
            if (conteudo == null || !conteudo.toLowerCase(Locale.ROOT)
                    .matches("application/x-www-form-urlencoded\\s*;\\s*charset=utf-8")) {
                responder(exchange, 415); return;
            }
            String tamanho = exchange.getRequestHeaders().getFirst("Content-Length");
            if (tamanho != null) {
                try {
                    long n = Long.parseLong(tamanho);
                    if (n < 0) { responder(exchange, 400); return; }
                    if (n > 16384) { responder(exchange, 413); return; }
                } catch (NumberFormatException e) { responder(exchange, 400); return; }
            }
            byte[] bytes = exchange.getRequestBody().readNBytes(16385);
            if (bytes.length > 16384) { responder(exchange, 413); return; }
            Map<String, String> campos;
            try { campos = formulario(bytes); }
            catch (IllegalArgumentException e) { responder(exchange, 400); return; }
            String tipo = campos.get("tipo");
            if (!arquivos.aceitaCliente(tipo)) { responder(exchange, 400); return; }
            String par = exchange.getRemoteAddress().getAddress().getHostAddress();
            String ip = par;
            String procedencia = "ip=par-direto; computador=declarado; ";
            if (campos.containsKey("ipOriginal")) {
                String token = exchange.getRequestHeaders().getFirst("X-Chat-Logs-Token");
                if (!tipo.equals("web") || tokenWeb.isEmpty() || token == null || !paresWeb.contains(par)
                        || !MessageDigest.isEqual(tokenWeb.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8))) {
                    responder(exchange, 403); return;
                }
                try { ip = ipNumerico(campos.get("ipOriginal")); }
                catch (IllegalArgumentException e) { responder(exchange, 400); return; }
                procedencia = "ip=encaminhado-glassfish; par=" + par + "; computador=executor-jsp; ";
            } else if (tipo.equals("web")) {
                procedencia = "ip=par-direto; origem-web=declarada; computador=declarado; ";
            }
            RegistroErro erro = new RegistroErro(campos.get("computador"), ip, campos.get("classe"), campos.get("metodo"),
                    procedencia + campos.get("excecao") + ": " + campos.get("mensagem"));
            try {
                arquivos.registrarCliente(tipo, erro).get(1500, TimeUnit.MILLISECONDS);
                responder(exchange, 204);
            } catch (ExecutionException | TimeoutException e) {
                // Pode persistir após timeout; clientes não repetem automaticamente.
                responder(exchange, 503);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } catch (IOException e) {
            // Rede encerrada/timeout não significa defeito interno do coletor.
        } catch (RuntimeException e) {
            Util.registrarErro("internal", "coletor", "-", getClass().getName(), "atender", e);
        } finally { if (admitido) admissoes.release(); }
    }

    private static String ipNumerico(String ip) {
        try {
            if (ip.contains(":")) {
                if (!ip.matches("[0-9a-fA-F:.]{2,64}")) throw new IllegalArgumentException();
                return InetAddress.getByName(ip).getHostAddress(); // literal IPv6: sem busca DNS
            }
            String[] partes = ip.split("\\.", -1);
            if (partes.length != 4) throw new IllegalArgumentException();
            for (String parte : partes) {
                if (!parte.matches("[0-9]{1,3}") || Integer.parseInt(parte) > 255) throw new IllegalArgumentException();
            }
            return ip;
        } catch (UnknownHostException e) { throw new IllegalArgumentException(e); }
    }

    private static Map<String, String> formulario(byte[] bytes) {
        for (byte b : bytes) if (b < 0) throw new IllegalArgumentException("Use percent-encoding UTF-8.");
        Map<String, Integer> limites = Map.of("versao", 1, "tipo", 32, "computador", 128, "classe", 256,
                "metodo", 128, "excecao", 256, "mensagem", 4096, "ipOriginal", 64);
        Map<String, String> campos = new HashMap<>();
        for (String par : new String(bytes, StandardCharsets.US_ASCII).split("&", -1)) {
            int igual = par.indexOf('=');
            if (igual < 1) throw new IllegalArgumentException();
            String chave = decodificar(par.substring(0, igual));
            String valor = decodificar(par.substring(igual + 1));
            if (!limites.containsKey(chave) || valor.length() > limites.get(chave)
                    || campos.putIfAbsent(chave, valor) != null) throw new IllegalArgumentException();
        }
        if (!"1".equals(campos.get("versao"))) throw new IllegalArgumentException();
        for (String campo : List.of("tipo", "computador", "classe", "metodo", "excecao", "mensagem")) {
            if (!campos.containsKey(campo) || campos.get(campo).isBlank()) throw new IllegalArgumentException();
        }
        if (!campos.get("tipo").matches("[a-z][a-z0-9_]{0,31}")) throw new IllegalArgumentException();
        return campos;
    }

    private static String decodificar(String texto) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c == '%') {
                if (i + 2 >= texto.length()) throw new IllegalArgumentException();
                int a = Character.digit(texto.charAt(++i), 16), b = Character.digit(texto.charAt(++i), 16);
                if (a < 0 || b < 0) throw new IllegalArgumentException();
                bytes.write(a * 16 + b);
            } else bytes.write(c == '+' ? ' ' : c);
        }
        try {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes.toByteArray())).toString();
        } catch (CharacterCodingException e) { throw new IllegalArgumentException(e); }
    }

    private static void responder(HttpExchange exchange, int codigo) throws IOException {
        exchange.sendResponseHeaders(codigo, -1);
    }

    public synchronized void pararServidor() {
        ativo = false;
        if (servidor != null) servidor.stop(1);
        if (tarefas != null) {
            tarefas.shutdown(); // nunca interromper persistências
            try { tarefas.awaitTermination(3, TimeUnit.SECONDS); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        servidor = null;
        tarefas = null;
    }

    public boolean isAtivo() { return ativo; }
    public synchronized int portaLocal() { return servidor == null ? porta : servidor.getAddress().getPort(); }
}
