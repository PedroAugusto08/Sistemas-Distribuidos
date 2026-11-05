import com.mycompany.chat_cliente_web.ChatWeb;
import java.io.*;
import java.lang.reflect.Field;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.text.html.HTMLEditorKit;
import pacote.*;

/** Regressões pós-auditoria. Somente arquivos temporários e portas efêmeras. */
public class CorrecoesAuditoriaTest {
    public static void main(String[] args) throws Exception {
        String caso = args.length == 0 ? "todos" : args[0];
        if (caso.equals("todos") || caso.equals("html")) testarFiltro();
        if (caso.equals("todos") || caso.equals("historico")) testarHistoricoGrande();
        if (caso.equals("todos") || caso.equals("desktop")) testarParadaNaPersistencia(false);
        if (caso.equals("todos") || caso.equals("web")) testarParadaNaPersistencia(true);
        System.out.println("PASSOU: correções pós-auditoria (" + caso + ").");
    }

    private static void testarFiltro() throws Exception {
        // Compara o contador com o próprio encoder do protocolo, inclusive nos limites.
        for (String texto : List.of("", "a\u0000\u007f\u0080\u07ff\u0800\uffff😀", "x".repeat(65535),
                "\u0000".repeat(32767) + "a", "😀".repeat(10922) + "abc")) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            new DataOutputStream(bytes).writeUTF(texto);
            verificar(MensagemHtml.tamanhoUtfModificado(texto) == bytes.size() - 2, "Contagem UTF modificado incorreta");
        }
        // Ambas as formas provocavam buscas/substrings repetidas no algoritmo anterior.
        String adversarial = "<".repeat(500000);
        for (String sufixo : List.of("", ">")) {
            FutureTask<String> trabalho = new FutureTask<>(() -> MensagemHtml.paraExibicao(adversarial + sufixo));
            Thread.ofVirtual().start(trabalho);
            String filtrada = trabalho.get(5, TimeUnit.SECONDS);
            verificar(filtrada.equals("&lt;".repeat(500000) + sufixo), "Filtro alterou texto adversarial");
        }
        for (String texto : List.of("a &lt; b &gt; c &amp; d<br>", "<b>Olá 😀</b><br>",
                "<br   />", "<<b>texto</b>", "<script><img src=x onerror=1></script>")) {
            String filtrada = MensagemHtml.paraExibicao(texto);
            verificar(!filtrada.contains("<script>") && !filtrada.contains("<img src=x"), "HTML inseguro");
            verificar(MensagemHtml.paraExibicao(filtrada).equals(filtrada), "Filtro não é idempotente");
        }
        verificar(MensagemHtml.paraExibicao("<<b>texto</b>").equals("&lt;<b>texto</b>"), "Tag válida após < inválido");
        verificar(MensagemHtml.paraExibicao("<br   />").equals("<br   />"), "Regressão em tag legítima");
        HTMLEditorKit kit = new HTMLEditorKit();
        var documento = kit.createDefaultDocument();
        kit.read(new StringReader(MensagemHtml.paraExibicao("a &lt; b &gt; c &amp; d<br>")), documento, 0);
        verificar(documento.getText(0, documento.getLength()).contains("a < b > c & d"), "Texto legítimo no Swing");
        System.out.println("PASSOU: 500 mil < com/sem > em até 5 s por caso; tags, segurança e texto Swing.");
    }

    private static void testarHistoricoGrande() throws Exception {
        Path arquivo = Files.createTempDirectory("chat-auditoria-historico-").resolve("historico.txt");
        String expansao = "<".repeat(20000) + "😀";
        String utfModificado = "😀".repeat(11000); // UTF-8 comum cabe; UTF modificado tem 66000 bytes.
        String normal = ChatWeb.montarMensagem("Web", "navy", "menino.png", "a < b > c & d 😀", "emojis/coracao.gif");
        String original = "antes<br>\n" + expansao + "\n" + utfModificado + "\n" + normal + "\n";
        Files.writeString(arquivo, original, StandardCharsets.UTF_8);
        RepositorioMensagens repositorio = new RepositorioMensagens(arquivo);
        instalarRepositorio(repositorio);
        configurarPortas();
        ServicoDesktop desktop = new ServicoDesktop(erro -> { throw new AssertionError(erro); });
        WebHttpService web = new WebHttpService();
        ByteArrayOutputStream diagnostico = new ByteArrayOutputStream();
        PrintStream anterior = System.err;
        try (PrintStream captura = new PrintStream(diagnostico, true, StandardCharsets.UTF_8)) {
            System.setErr(captura);
            desktop.iniciar();
            web.iniciar();
            try (Socket socket = new Socket("127.0.0.1", Util.PortaEnvioDesktop)) {
                socket.setSoTimeout(8000);
                ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
                verificar(input.readUTF().equals("antes<br>"), "Primeira mensagem perdida");
                for (int i = 0; i < 2; i++) {
                    String aviso = input.readUTF();
                    verificar(aviso.contains("excede o limite de exibição"), "Aviso de limite ausente");
                    MensagemHtml.validar(aviso);
                }
                verificar(input.readUTF().equals(normal), "Mensagem seguinte/HTML legítimo prejudicado");
                repositorio.adicionarMensagem("depois-ao-vivo<br>");
                verificar(input.readUTF().equals("depois-ao-vivo<br>"), "Conexão caiu após mensagem grande");
            }
            verificar(repositorio.consultarMensagens(1).get(0).equals(expansao), "RAM perdeu original");
            verificar(Files.readString(arquivo).startsWith(original), "TXT original modificado");
            String html = ChatWeb.consultar(endpoint());
            verificar(html.contains(MensagemHtml.paraExibicao(expansao)) && html.contains(utfModificado), "Web perdeu conteúdo completo");
            String log = diagnostico.toString(StandardCharsets.UTF_8);
            verificar(log.contains("80006") && log.contains("66000") && log.contains("índice"), "Diagnóstico sem tamanho/índice");
        } finally {
            desktop.pararServidor();
            web.pararServidor();
            System.setErr(anterior);
        }
        System.out.println("PASSOU: overflow após filtro e por UTF modificado; aviso, sequência TCP, Web e TXT preservados.");
    }

    private static void testarParadaNaPersistencia(boolean ehWeb) throws Exception {
        Path arquivo = Files.createTempDirectory("chat-auditoria-parada-").resolve("historico.txt");
        RepositorioPausado repositorio = new RepositorioPausado(arquivo);
        instalarRepositorio(repositorio);
        configurarPortas();
        ServicoDesktop desktop = new ServicoDesktop(erro -> { throw new AssertionError(erro); });
        WebHttpService web = new WebHttpService();
        try (ExecutorService controle = Executors.newVirtualThreadPerTaskExecutor(); Socket cliente = new Socket()) {
            desktop.iniciar();
            web.iniciar();
            String mensagem = "gravacao-pausada-" + (ehWeb ? "web" : "desktop") + "<br>";
            // O cliente HTTP não aguarda resposta: a parada pode fechar a rede após o POST recebido.
            cliente.connect(new InetSocketAddress("127.0.0.1", ehWeb ? Util.PortaWeb : Util.PortaRecepcaoDesktop));
            if (ehWeb) {
                byte[] dados = mensagem.getBytes(StandardCharsets.UTF_8);
                cliente.getOutputStream().write(("POST /mensagens HTTP/1.1\r\nHost: localhost\r\n"
                        + "Content-Type: text/plain; charset=UTF-8\r\nContent-Length: " + dados.length + "\r\n\r\n")
                        .getBytes(StandardCharsets.US_ASCII));
                cliente.getOutputStream().write(dados);
                cliente.getOutputStream().flush();
            } else {
                ObjectOutputStream output = new ObjectOutputStream(cliente.getOutputStream());
                output.writeUTF(mensagem);
                output.flush();
            }
            verificar(repositorio.entrou.await(5, TimeUnit.SECONDS), "Não iniciou persistência");
            Future<?> parada = controle.submit(ehWeb ? web::pararServidor : desktop::pararServidor);
            try {
                // A pausa dura além do prazo de encerramento: exige retorno limitado sem interrupção.
                parada.get(8, TimeUnit.SECONDS);
                verificar(!repositorio.interrompido.get(), "Parada interrompeu tarefa de persistência");
                verificar(!ehWeb ? !desktop.isAtivo() && web.isAtivo() : !web.isAtivo() && desktop.isAtivo(), "Independência perdida");
                if (ehWeb) web.iniciar(); else desktop.iniciar();
                verificar(Util.obterRepositorio() == repositorio, "Repositório recriado na reativação");
            } finally { repositorio.liberar.countDown(); }
            verificar(repositorio.terminou.await(5, TimeUnit.SECONDS), "Gravação não terminou");
            verificar(repositorio.falha == null, "Gravação falhou: " + repositorio.falha);
            verificar(repositorio.consultarMensagens(0).equals(List.of(mensagem)), "RAM incorreta após parada");
            verificar(Files.readAllLines(arquivo).equals(List.of(mensagem)), "TXT incorreto após parada");
            ChatWeb.enviar(endpoint(), "apos-parada-http<br>");
            try (Socket socket = new Socket("127.0.0.1", Util.PortaRecepcaoDesktop);
                    ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream())) {
                output.writeUTF("apos-parada-tcp<br>");
            }
            long fim = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (repositorio.consultarMensagens(0).size() < 3 && System.nanoTime() < fim) Thread.sleep(20);
            verificar(repositorio.consultarMensagens(0).size() == 3, "Repositório inutilizável após parada");
            verificar(Files.readAllLines(arquivo).equals(repositorio.consultarMensagens(0)), "RAM/TXT divergentes");
        } finally {
            repositorio.liberar.countDown();
            desktop.pararServidor();
            web.pararServidor();
        }
        System.out.println("PASSOU: parada " + (ehWeb ? "Web" : "Desktop") + " concorrente com persistência e reativação antes do término.");
    }

    /** Pausa determinística dentro da chamada sincronizada, antes da E/S real. */
    private static class RepositorioPausado extends RepositorioMensagens {
        final CountDownLatch entrou = new CountDownLatch(1);
        final CountDownLatch liberar = new CountDownLatch(1);
        final CountDownLatch terminou = new CountDownLatch(1);
        final AtomicBoolean interrompido = new AtomicBoolean();
        volatile Exception falha;

        RepositorioPausado(Path arquivo) throws IOException { super(arquivo); }

        @Override
        public synchronized void adicionarMensagem(String mensagem) throws IOException {
            if (!mensagem.startsWith("gravacao-pausada-")) {
                super.adicionarMensagem(mensagem);
                return;
            }
            entrou.countDown();
            try {
                boolean liberado = false;
                while (!liberado) {
                    try { liberar.await(); liberado = true; }
                    catch (InterruptedException e) { interrompido.set(true); }
                }
                if (interrompido.get()) Thread.currentThread().interrupt();
                super.adicionarMensagem(mensagem);
            } catch (IOException | RuntimeException e) { falha = e; throw e; }
            finally { terminou.countDown(); }
        }
    }

    private static void instalarRepositorio(RepositorioMensagens repositorio) throws Exception {
        // Injeção restrita ao teste: não se acrescenta configuração ao código de produção.
        Field campo = Util.class.getDeclaredField("repositorioMensagens");
        campo.setAccessible(true);
        campo.set(null, repositorio);
    }

    private static void configurarPortas() throws IOException {
        try (ServerSocket recepcao = new ServerSocket(0); ServerSocket envio = new ServerSocket(0);
                ServerSocket web = new ServerSocket(0)) {
            Util.PortaRecepcaoDesktop = recepcao.getLocalPort();
            Util.PortaEnvioDesktop = envio.getLocalPort();
            Util.PortaWeb = web.getLocalPort();
        }
        Util.EnderecoWeb = "127.0.0.1";
    }

    private static String endpoint() { return "http://127.0.0.1:" + Util.PortaWeb + "/mensagens"; }

    private static void verificar(boolean condicao, String erro) {
        if (!condicao) throw new AssertionError(erro);
    }
}
