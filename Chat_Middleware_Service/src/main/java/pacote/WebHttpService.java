package pacote;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class WebHttpService {
    private static final int LIMITE_CORPO = 65535;
    private HttpServer servidor;
    private ExecutorService tarefas;
    private volatile boolean ativo;

    public synchronized void iniciar() throws IOException {
        if (ativo) return;
        Util.inicializarRepositorio();
        RepositorioMensagens repositorio = Util.obterRepositorio();
        Util.configurarHttp();
        try {
            // Guarda a referência antes do bind para poder liberar inclusive falhas de porta.
            servidor = HttpServer.create();
            servidor.bind(new InetSocketAddress(Util.EnderecoWeb, Util.PortaWeb), 32);
            // Cada requisição pode aguardar rede ou disco sem ocupar uma thread dedicada por conexão.
            tarefas = Executors.newVirtualThreadPerTaskExecutor();
            servidor.setExecutor(tarefas);
            servidor.createContext("/mensagens", exchange -> atender(exchange, repositorio));
            servidor.start();
            ativo = true;
        } catch (IOException | RuntimeException e) {
            pararServidor();
            Util.registrarErro("web", "inicializacao", "-", getClass().getName(), "iniciar", e);
            throw e;
        }
    }

    private void atender(HttpExchange exchange, RepositorioMensagens repositorio) {
        try (exchange) {
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
            // A API é servidor-servidor; não aceita chamadas iniciadas por páginas externas.
            if (exchange.getRequestHeaders().containsKey("Origin")) {
                responder(exchange, 403, "Origem não permitida.", "text/plain");
            } else if (!exchange.getRequestURI().getRawPath().equals("/mensagens")
                    || exchange.getRequestURI().getRawQuery() != null) {
                responder(exchange, 404, "Recurso inexistente.", "text/plain");
            } else if (exchange.getRequestMethod().equals("GET")) {
                StringBuilder html = new StringBuilder();
                for (String mensagem : repositorio.consultarMensagens(0)) {
                    html.append(MensagemHtml.paraExibicao(mensagem)).append('\n');
                }
                responder(exchange, 200, html.toString(), "text/html");
            } else if (exchange.getRequestMethod().equals("POST")) {
                receber(exchange, repositorio);
            } else {
                exchange.getResponseHeaders().set("Allow", "GET, POST");
                responder(exchange, 405, "Método não permitido.", "text/plain");
            }
        } catch (IOException e) {
            // Desconexão/timeout: não repetir POST; a persistência pode já ter ocorrido.
        } catch (RuntimeException e) {
            Util.registrarErro("web", "adaptador-web", exchange.getRemoteAddress().getAddress().getHostAddress(),
                    getClass().getName(), "atender", e);
        }
    }

    private void receber(HttpExchange exchange, RepositorioMensagens repositorio) throws IOException {
        String tipo = exchange.getRequestHeaders().getFirst("Content-Type");
        if (tipo == null || !tipo.toLowerCase(Locale.ROOT)
                .matches("text/plain\\s*;\\s*charset=utf-8")) {
            responder(exchange, 415, "Use text/plain; charset=UTF-8.", "text/plain");
            return;
        }
        String tamanho = exchange.getRequestHeaders().getFirst("Content-Length");
        if (tamanho != null) {
            try {
                long numero = Long.parseLong(tamanho);
                if (numero < 0) throw new NumberFormatException();
                if (numero > LIMITE_CORPO) {
                    responder(exchange, 413, "Mensagem muito grande.", "text/plain");
                    return;
                }
            } catch (NumberFormatException e) {
                responder(exchange, 400, "Tamanho inválido.", "text/plain");
                return;
            }
        }
        byte[] corpo = exchange.getRequestBody().readNBytes(LIMITE_CORPO + 1);
        if (corpo.length > LIMITE_CORPO) {
            responder(exchange, 413, "Mensagem muito grande.", "text/plain");
            return;
        }
        String mensagem;
        try {
            mensagem = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(corpo)).toString();
            MensagemHtml.validar(mensagem);
        } catch (CharacterCodingException | IllegalArgumentException e) {
            responder(exchange, 400, "Mensagem inválida ou incompatível com o protocolo Desktop.", "text/plain");
            return;
        }
        try {
            repositorio.adicionarMensagem(mensagem);
        } catch (IOException e) {
            Util.registrarErro("web", "adaptador-web", exchange.getRemoteAddress().getAddress().getHostAddress(),
                    getClass().getName(), "receber", e);
            responder(exchange, 500, "Não foi possível gravar a mensagem.", "text/plain");
            return;
        }
        exchange.sendResponseHeaders(204, -1);
    }

    private static void responder(HttpExchange exchange, int status, String texto, String tipo) throws IOException {
        byte[] dados = texto.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", tipo + "; charset=UTF-8");
        exchange.sendResponseHeaders(status, dados.length == 0 ? -1 : dados.length);
        if (dados.length != 0) exchange.getResponseBody().write(dados);
    }

    public synchronized void pararServidor() {
        ativo = false;
        if (servidor != null) servidor.stop(1);
        if (tarefas != null) {
            // stop fecha a rede. Persistências recebidas devem terminar sem interrupção.
            tarefas.shutdown();
            try {
                if (!tarefas.awaitTermination(3, TimeUnit.SECONDS)) {
                    System.err.println("HTTP: persistência ainda em andamento após fechamento da rede.");
                }
            }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        servidor = null;
        tarefas = null;
    }

    public boolean isAtivo() { return ativo; }
}
