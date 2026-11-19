import com.mycompany.chat_cliente_web.ChatWeb;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import pacote.*;

/** Sem JUnit/dependências: executável com JDK 21+, usando apenas TXT temporário. */
public class ServicosIntegracaoTest {
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2)).build();
    private static String endpoint;

    public static void main(String[] args) throws Exception {
        Path pasta = Files.createTempDirectory("chat-servicos-teste-");
        Path arquivo = pasta.resolve("historico.txt");
        Files.writeString(arquivo, "<b>Antiga</b><br>\n<script>alert(1)</script>\n", StandardCharsets.UTF_8);
        Util.PathRepDesktop = arquivo.toString();
        Util.inicializarRepositorio();
        RepositorioMensagens repositorio = Util.obterRepositorio();
        Util.PortaRecepcaoDesktop = portaLivre();
        do { Util.PortaEnvioDesktop = portaLivre(); }
        while (Util.PortaEnvioDesktop == Util.PortaRecepcaoDesktop);
        do { Util.PortaWeb = portaLivre(); }
        while (Util.PortaWeb == Util.PortaEnvioDesktop || Util.PortaWeb == Util.PortaRecepcaoDesktop);
        Util.EnderecoWeb = "127.0.0.1";
        endpoint = "http://127.0.0.1:" + Util.PortaWeb + "/mensagens";
        List<Exception> falhas = new CopyOnWriteArrayList<>();
        ServicoDesktop desktop = new ServicoDesktop(falhas::add);
        WebHttpService web = new WebHttpService();
        try {
            testarHtml();
            try (ServerSocket ocupado = new ServerSocket(Util.PortaEnvioDesktop)) {
                try { desktop.iniciar(); throw new AssertionError("Aceitou porta ocupada"); }
                catch (IOException esperado) { }
                verificar(!desktop.isAtivo(), "Desktop falso ativo");
                try (ServerSocket liberado = new ServerSocket(Util.PortaRecepcaoDesktop)) { }
            }
            try (ServerSocket ocupado = new ServerSocket(Util.PortaWeb)) {
                try { web.iniciar(); throw new AssertionError("HTTP aceitou porta ocupada"); }
                catch (IOException esperado) { }
                verificar(!web.isAtivo(), "Web falso ativo");
            }
            desktop.iniciar();
            web.iniciar();
            verificar(desktop.isAtivo() && web.isAtivo(), "Serviços não iniciaram");
            String antiga = get();
            verificar(antiga.contains("<b>Antiga</b>") && !antiga.contains("<script>"), "Histórico inseguro");
            verificar(Files.readString(arquivo).contains("<script>"), "Reescreveu histórico antigo");
            testarHttpInvalido();
            testarHttpLento();
            testarRecepcaoLenta(repositorio);
            testarComunicacaoCruzada();
            testarSimultaneos(repositorio);
            testarPersistencia(arquivo, repositorio);
            testarAvisoManutencao(desktop, repositorio);
            desktop.pararServidor();
            verificar(!desktop.isAtivo(), "Desktop não parou");
            verificar(post("somente-web<br>").statusCode() == 204, "Desktop afetou Web");
            for (int i = 0; i < 5; i++) {
                desktop.iniciar();
                web.pararServidor();
                verificar(desktop.isAtivo() && !web.isAtivo(), "Web afetou Desktop");
                int tamanho = repositorio.consultarMensagens(0).size();
                enviarTcp("somente-desktop-" + i + "<br>");
                aguardarTamanho(repositorio, tamanho + 1);
                try { get(); throw new AssertionError("HTTP respondeu desligado"); }
                catch (IOException esperado) { }
                web.iniciar();
                desktop.pararServidor();
                verificar(get().contains("somente-desktop-" + i), "Histórico perdido na reativação");
                verificar(Util.obterRepositorio() == repositorio, "Recriou repositório");
            }
            desktop.iniciar();
            testarParadaComClienteLento(desktop, repositorio);
            verificar(falhas.isEmpty(), "Listener falhou: " + falhas);
            System.out.println("PASSOU: HTTP, TCP, concorrência, HTML, persistência, rollback de bind e reativações.");
            System.out.println("Histórico de teste: " + arquivo);
        } finally {
            desktop.pararServidor();
            web.pararServidor();
        }
    }

    private static void testarHtml() throws Exception {
        String mensagem = ChatWeb.montarMensagem("<script>", "navy", "menino.png", "<img onerror=1> & olá 😀", "emojis/choro.gif");
        verificar(mensagem.contains("&lt;script&gt;") && mensagem.contains("&lt;img onerror=1&gt;"), "Não escapou texto");
        MensagemHtml.validar(mensagem);
        for (String nome : List.of("baby", "cat", "JJJ", "sigma", "thinking", "shrek", "cristiano")) {
            MensagemHtml.validar("<img src='/figurinhas/stickers/" + nome + ".gif' width='128' height='128'><br>");
        }
        MensagemHtml.validar("<font color='#0000FF'>Nick</font><b><u> Grita: </u></b><font color='tomato' size='+1'>😀 &lt;3</font><br>");
        verificar(!ChatWeb.perfilValido("nick", "red' onmouseover='x", "menino.png"), "Aceitou atributo livre");
        for (String invalida : List.of("", "<script>x</script>", "<img src='http://externo/x'>", "<b onclick='x'>x</b>", "😀".repeat(11000))) {
            try { MensagemHtml.validar(invalida); throw new AssertionError("Aceitou mensagem inválida"); }
            catch (IllegalArgumentException esperado) { }
        }
        System.out.println("PASSOU: escape, tags, atributos, stickers e limite UTF modificado.");
    }

    private static void testarHttpInvalido() throws Exception {
        verificar(getResponse(endpoint + "/extra").statusCode() == 404, "Prefixo aceito");
        verificar(getResponse(endpoint + "?x=1").statusCode() == 404, "Query aceita");
        HttpResponse<String> metodo = HTTP.send(HttpRequest.newBuilder(URI.create(endpoint)).DELETE().build(), HttpResponse.BodyHandlers.ofString());
        verificar(metodo.statusCode() == 405 && metodo.headers().firstValue("Allow").orElse("").equals("GET, POST"), "Método/Allow");
        verificar(post("").statusCode() == 400, "Corpo vazio");
        verificar(post("<script>x</script>").statusCode() == 400, "HTML arbitrário");
        verificar(post("x".repeat(65536)).statusCode() == 413, "Corpo grande");
        verificar(post("😀".repeat(11000)).statusCode() == 400, "UTF modificado incompatível");
        HttpRequest.Builder base = HttpRequest.newBuilder(URI.create(endpoint)).timeout(Duration.ofSeconds(8));
        verificar(HTTP.send(base.POST(HttpRequest.BodyPublishers.ofString("x")).build(), HttpResponse.BodyHandlers.ofString()).statusCode() == 415, "Tipo inválido");
        verificar(HTTP.send(HttpRequest.newBuilder(URI.create(endpoint)).header("Content-Type", "text/plain; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofByteArray(new byte[]{(byte) 0xc3, 0x28})).build(), HttpResponse.BodyHandlers.ofString()).statusCode() == 400, "UTF-8 malformado");
        verificar(HTTP.send(HttpRequest.newBuilder(URI.create(endpoint)).header("Origin", "http://externo")
                .GET().build(), HttpResponse.BodyHandlers.ofString()).statusCode() == 403, "Origem externa aceita");
        // Sem Content-Length: limite deve valer também com transferência chunked.
        verificar(HTTP.send(HttpRequest.newBuilder(URI.create(endpoint)).header("Content-Type", "text/plain; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofInputStream(() -> new ByteArrayInputStream(new byte[65536]))).build(),
                HttpResponse.BodyHandlers.ofString()).statusCode() == 413, "Chunked sem limite");
        verificar(getResponse(endpoint).headers().firstValue("Cache-Control").orElse("").equals("no-store"), "Cache ativo");
        System.out.println("PASSOU: HTTP inválido, limites e codificação.");
    }

    private static void testarRecepcaoLenta(RepositorioMensagens repositorio) throws Exception {
        int antes = repositorio.consultarMensagens(0).size();
        try (Socket lento = new Socket("127.0.0.1", Util.PortaRecepcaoDesktop)) {
            lento.setSoTimeout(7000);
            enviarTcp("cliente-normal<br>");
            aguardarTamanho(repositorio, antes + 1);
            verificar(lento.getInputStream().read() == -1, "Cliente lento não expirou");
        }
        try (Socket invalido = new Socket("127.0.0.1", Util.PortaRecepcaoDesktop)) {
            invalido.getOutputStream().write(new byte[]{0, 0, 0, 0});
        }
        enviarTcp("apos-cliente-invalido<br>");
        aguardarTamanho(repositorio, antes + 2);
        System.out.println("PASSOU: cliente TCP lento/inválido não bloqueia outros.");
    }

    private static void testarHttpLento() throws Exception {
        try (Socket lento = new Socket("127.0.0.1", Util.PortaWeb)) {
            lento.setSoTimeout(15000);
            lento.getOutputStream().write(("POST /mensagens HTTP/1.1\r\nHost: localhost\r\n"
                    + "Content-Type: text/plain; charset=UTF-8\r\nContent-Length: 100\r\n\r\nx")
                    .getBytes(StandardCharsets.US_ASCII));
            verificar(getResponse(endpoint).statusCode() == 200, "HTTP lento bloqueou GET");
            try {
                verificar(lento.getInputStream().read() == -1, "HTTP incompleto não expirou");
            } catch (SocketException esperado) { /* Reset também encerra o cliente lento. */ }
        }
        verificar(getResponse(endpoint).statusCode() == 200, "HTTP não recuperou após timeout");
        System.out.println("PASSOU: timeout HTTP de corpo incompleto sem bloquear outros clientes.");
    }

    private static void testarAvisoManutencao(ServicoDesktop desktop, RepositorioMensagens repositorio) throws Exception {
        try (Socket socket = new Socket("127.0.0.1", Util.PortaEnvioDesktop);
                ExecutorService tarefa = Executors.newVirtualThreadPerTaskExecutor()) {
            socket.setSoTimeout(5000);
            ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
            int quantidade = repositorio.consultarMensagens(0).size();
            for (int i = 0; i < quantidade; i++) input.readUTF();
            Future<?> parada = tarefa.submit(desktop::pararServidor);
            verificar(input.readUTF().equals("<b><font color='red'>Em manutenção...</font></b><br>"), "Perdeu aviso de manutenção");
            parada.get(8, TimeUnit.SECONDS);
        }
        desktop.iniciar();
        System.out.println("PASSOU: aviso de manutenção TCP preservado.");
    }

    private static void testarComunicacaoCruzada() throws Exception {
        try (Socket socket = new Socket("127.0.0.1", Util.PortaEnvioDesktop)) {
            socket.setSoTimeout(5000);
            ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
            String mensagem = ChatWeb.montarMensagem("Web", "navy", "menino.png", "Olá 😀", "emojis/coracao.gif");
            ChatWeb.enviar(endpoint, mensagem);
            boolean encontrou = false;
            for (int i = 0; i < 20; i++) {
                if (input.readUTF().equals(mensagem)) { encontrou = true; break; }
            }
            verificar(encontrou, "POST não chegou ao TCP");
        }
        enviarTcp("desktop-para-web<br>");
        long fim = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!ChatWeb.consultar(endpoint).contains("desktop-para-web")) {
            if (System.nanoTime() > fim) throw new AssertionError("TCP não chegou ao GET");
            Thread.sleep(20);
        }
        System.out.println("PASSOU: troca cruzada pelo mesmo repositório.");
    }

    private static void testarSimultaneos(RepositorioMensagens repositorio) throws Exception {
        int antes = repositorio.consultarMensagens(0).size();
        try (ExecutorService tarefas = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> resultados = new ArrayList<>();
            for (int i = 0; i < 40; i++) {
                final int indice = i;
                resultados.add(tarefas.submit(() -> {
                    String mensagem = "simultanea-" + indice + "<br>";
                    if (indice % 2 == 0) enviarTcp(mensagem);
                    else verificar(post(mensagem).statusCode() == 204, "POST concorrente falhou");
                    return null;
                }));
            }
            for (Future<?> resultado : resultados) resultado.get(15, TimeUnit.SECONDS);
        }
        aguardarTamanho(repositorio, antes + 40);
        List<String> novas = repositorio.consultarMensagens(antes);
        verificar(novas.size() == 40 && new HashSet<>(novas).size() == 40, "Perda/duplicação concorrente");
        System.out.println("PASSOU: 40 envios simultâneos TCP/HTTP sem perda/duplicação.");
    }

    private static void testarPersistencia(Path arquivo, RepositorioMensagens repositorio) throws Exception {
        int antes = repositorio.consultarMensagens(0).size();
        Path salvo = arquivo.resolveSibling("salvo.txt");
        Files.move(arquivo, salvo);
        try {
            verificar(post("nao-gravar<br>").statusCode() == 500, "Confirmou falha de persistência");
            verificar(repositorio.consultarMensagens(0).size() == antes, "RAM mudou após falha");
            verificar(get().contains("simultanea-"), "Consulta depende do TXT");
        } finally { Files.move(salvo, arquivo); }
        verificar(post("persistiu-utf8-á-😀<br>").statusCode() == 204, "Não recuperou persistência");
        verificar(Files.readString(arquivo).contains("persistiu-utf8-á-😀"), "Confirmou antes de persistir");
        verificar(Files.readAllLines(arquivo).equals(repositorio.consultarMensagens(0)), "RAM/TXT divergem");
        System.out.println("PASSOU: falha e recuperação de persistência exclusivamente em TXT temporário.");
    }

    private static void testarParadaComClienteLento(ServicoDesktop desktop, RepositorioMensagens repositorio) throws Exception {
        try (Socket lento = new Socket()) {
            lento.setReceiveBufferSize(1024);
            lento.connect(new InetSocketAddress("127.0.0.1", Util.PortaEnvioDesktop));
            // O cliente não lê. Outros clientes devem continuar recebendo.
            String grande = "x".repeat(60000) + "<br>";
            for (int i = 0; i < 80; i++) repositorio.adicionarMensagem(grande);
            try (Socket normal = new Socket("127.0.0.1", Util.PortaEnvioDesktop)) {
                normal.setSoTimeout(5000);
                verificar(new ObjectInputStream(normal.getInputStream()).readUTF().contains("Antiga"), "Cliente lento bloqueou envio");
            }
            long inicio = System.nanoTime();
            desktop.pararServidor();
            verificar(System.nanoTime() - inicio < TimeUnit.SECONDS.toNanos(8), "Encerramento TCP travou");
            desktop.iniciar();
            verificar(desktop.isAtivo(), "Não reativou após cliente lento");
        }
        System.out.println("PASSOU: cliente sem leitura, parada limitada e reativação.");
    }

    private static int portaLivre() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) { return socket.getLocalPort(); }
    }

    private static void enviarTcp(String mensagem) throws IOException {
        try (Socket socket = new Socket("127.0.0.1", Util.PortaRecepcaoDesktop);
                ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream())) {
            output.writeUTF(mensagem);
            output.flush();
        }
    }

    private static HttpResponse<String> post(String mensagem) throws Exception {
        return HTTP.send(HttpRequest.newBuilder(URI.create(endpoint)).timeout(Duration.ofSeconds(8))
                .header("Content-Type", "text/plain; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(mensagem, StandardCharsets.UTF_8)).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static HttpResponse<String> getResponse(String endereco) throws Exception {
        return HTTP.send(HttpRequest.newBuilder(URI.create(endereco)).timeout(Duration.ofSeconds(8)).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static String get() throws Exception {
        HttpResponse<String> resposta = getResponse(endpoint);
        verificar(resposta.statusCode() == 200, "GET falhou");
        return resposta.body();
    }

    private static void aguardarTamanho(RepositorioMensagens repositorio, int tamanho) throws Exception {
        long fim = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (repositorio.consultarMensagens(0).size() < tamanho) {
            if (System.nanoTime() > fim) throw new AssertionError("Mensagem não recebida");
            Thread.sleep(20);
        }
    }

    private static void verificar(boolean condicao, String mensagem) {
        if (!condicao) throw new AssertionError(mensagem);
    }
}
