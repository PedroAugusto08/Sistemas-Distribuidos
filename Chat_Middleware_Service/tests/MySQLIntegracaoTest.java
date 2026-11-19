import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import pacote.RepositorioMensagensMySQL;
import pacote.ServicoDesktop;
import pacote.Util;
import pacote.WebHttpService;

/** Execute apenas contra um banco reservado para testes, com a tabela de banco.sql. */
public class MySQLIntegracaoTest {
    public static void main(String[] args) throws Exception {
        try {
            new RepositorioMensagensMySQL(null, null, null);
            throw new AssertionError("Configuração ausente foi aceita");
        } catch (IOException esperado) { }

        // DriverManager apenas localiza o driver; não abre conexão.
        java.sql.DriverManager.getDriver("jdbc:mysql://localhost:3306/chat_teste");
        if (args.length == 1 && args[0].equals("--verificar-driver")) {
            System.out.println("PASSOU: driver JDBC no classpath e configuração obrigatória.");
            return;
        }

        String url = System.getenv("CHAT_DB_URL");
        if (url == null || !url.matches("jdbc:mysql://[^/?]+/chat_teste(?:\\?.*)?")) {
            throw new IllegalArgumentException("Configure CHAT_DB_URL apontando ao banco chat_teste.");
        }
        String usuario = System.getenv("CHAT_DB_USER");
        String senha = System.getenv("CHAT_DB_PASSWORD");
        System.setProperty("chat.logs.dir", Files.createTempDirectory("chat-mysql-logs-").toString());
        Util.inicializarRepositorio();
        var repositorio = Util.obterRepositorio();
        if (!(repositorio instanceof RepositorioMensagensMySQL)) throw new AssertionError("Middleware não usa MySQL");
        var antes = repositorio.consultarMensagens(0);
        try {
            repositorio.adicionarMensagem(null); // NOT NULL deve rejeitar sem alterar o histórico.
            throw new AssertionError("Confirmou gravação rejeitada pelo banco");
        } catch (IOException esperado) { }
        if (!repositorio.consultarMensagens(0).equals(antes)) throw new AssertionError("RAM mudou após falha SQL");
        String prefixo = UUID.randomUUID().toString();
        var esperadas = new ArrayList<>(List.of(prefixo + " acentos: ação, ç e 😀\nsegunda linha"));
        repositorio.adicionarMensagem(esperadas.getFirst());
        try (var tarefas = Executors.newVirtualThreadPerTaskExecutor()) {
            var resultados = new ArrayList<java.util.concurrent.Future<?>>();
            for (int i = 0; i < 12; i++) {
                String mensagem = prefixo + " concorrente " + i;
                esperadas.add(mensagem);
                resultados.add(tarefas.submit(() -> { repositorio.adicionarMensagem(mensagem); return null; }));
            }
            for (var resultado : resultados) resultado.get();
        }
        var novas = repositorio.consultarMensagens(antes.size());
        if (novas.size() != esperadas.size() || !novas.containsAll(esperadas)) {
            throw new AssertionError("Mensagens perdidas, duplicadas ou alteradas");
        }
        testarServicos(prefixo);
        var recarregado = new RepositorioMensagensMySQL(url, usuario, senha);
        if (!recarregado.consultarMensagens(0).equals(repositorio.consultarMensagens(0))) {
            throw new AssertionError("Histórico MySQL divergiu após reinicialização");
        }
        System.out.println("PASSOU: MySQL, rejeição SQL, Unicode, concorrência, TCP/HTTP e recarga ordenada.");
    }

    private static void testarServicos(String prefixo) throws Exception {
        Util.PortaRecepcaoDesktop = 0;
        Util.PortaEnvioDesktop = 0;
        Util.PortaWeb = 0;
        Util.EnderecoWeb = "127.0.0.1";
        var desktop = new ServicoDesktop(erro -> { throw new AssertionError(erro); });
        var web = new WebHttpService();
        String mensagemWeb = prefixo + " web 😀<br>";
        String mensagemTcp = prefixo + " desktop ç<br>";
        try (var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build()) {
            desktop.iniciar();
            web.iniciar();
            var endpoint = URI.create("http://127.0.0.1:" + web.portaLocal() + "/mensagens");
            var resposta = http.send(HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "text/plain; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(mensagemWeb)).build(), HttpResponse.BodyHandlers.ofString());
            if (resposta.statusCode() != 204) throw new AssertionError("POST não persistiu no MySQL");
            try (var socket = new Socket("127.0.0.1", desktop.portaRecepcao());
                    var output = new ObjectOutputStream(socket.getOutputStream())) {
                output.writeUTF(mensagemTcp);
                output.flush();
            }
            long prazo = System.nanoTime() + Duration.ofSeconds(10).toNanos();
            while (!Util.obterRepositorio().consultarMensagens(0).contains(mensagemTcp)) {
                if (System.nanoTime() > prazo) throw new AssertionError("TCP não persistiu no MySQL");
                Thread.sleep(20);
            }
            var historico = http.send(HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(10)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            if (historico.statusCode() != 200 || !historico.body().contains(mensagemWeb) || !historico.body().contains(mensagemTcp)) {
                throw new AssertionError("GET não exibiu as mensagens cruzadas");
            }
            try (var socket = new Socket("127.0.0.1", desktop.portaEnvio())) {
                socket.setSoTimeout(5000);
                var input = new ObjectInputStream(socket.getInputStream());
                boolean recebeuWeb = false, recebeuTcp = false;
                int quantidade = Util.obterRepositorio().consultarMensagens(0).size();
                for (int i = 0; i < quantidade; i++) {
                    String mensagem = input.readUTF();
                    recebeuWeb |= mensagemWeb.equals(mensagem);
                    recebeuTcp |= mensagemTcp.equals(mensagem);
                }
                if (!recebeuWeb || !recebeuTcp) throw new AssertionError("Desktop não recebeu as mensagens cruzadas");
            }
        } finally {
            desktop.pararServidor();
            web.pararServidor();
        }
    }
}
