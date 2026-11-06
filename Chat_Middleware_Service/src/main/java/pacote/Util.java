package pacote;

import java.io.IOException;
import java.nio.file.Path;

public class Util {
    public static int PortaRecepcaoDesktop = 6662;
    public static int PortaEnvioDesktop = 6661;
    public static String PathRepDesktop = "C:\\Dados\\RepositorioDesktop.txt";

    private static RepositorioMensagens repositorioMensagens;

    public static synchronized void inicializarRepositorio() throws IOException {
        if (repositorioMensagens == null) {
            try { repositorioMensagens = new RepositorioMensagens(Path.of(PathRepDesktop)); }
            catch (IOException | RuntimeException e) {
                registrarErro("internal", "inicializacao", "-", Util.class.getName(), "inicializarRepositorio", e);
                throw e;
            }
        }
    }

    public static synchronized RepositorioMensagens obterRepositorio() {
        if (repositorioMensagens == null) {
            throw new IllegalStateException("Histórico de mensagens não inicializado.");
        }
        return repositorioMensagens;
    }

    private static ArquivadorErros logs;
    private static final Object BLOQUEIO_LOGS = new Object();

    public static ArquivadorErros obterLogs() {
        synchronized (BLOQUEIO_LOGS) {
            if (logs == null) {
                logs = new ArquivadorErros(Path.of(System.getProperty("chat.logs.dir", "C:\\Dados\\logs")));
                Runtime.getRuntime().addShutdownHook(new Thread(logs::close, "encerrar-logs"));
            }
            return logs;
        }
    }

    /** Melhor esforço, sem disco no chamador e sem recursão em falhas. */
    public static void registrarErro(String servico, String cliente, String ip, String classe, String metodo, Exception erro) {
        if (ip == null || ip.isBlank() || ip.equals("-")) {
            ip = "NÃO APLICÁVEL";
            cliente = cliente == null || cliente.isBlank() || cliente.equals("-")
                    ? "INTERNO" : "INTERNO (" + cliente + ")";
        }
        try { obterLogs().registrarMiddleware(servico, RegistroErro.de(cliente, ip, classe, metodo, erro)); }
        catch (RuntimeException e) { System.err.println("Logging indisponível (configuração)."); }
    }

    public static void configurarHttp() {
        definirPadrao("sun.net.httpserver.maxReqTime", "5");
        definirPadrao("sun.net.httpserver.maxRspTime", "5");
        definirPadrao("jdk.httpserver.maxConnections", "128");
        definirPadrao("sun.net.httpserver.maxReqHeaderSize", "8192");
        definirPadrao("sun.net.httpserver.drainAmount", "0");
    }

    private static void definirPadrao(String propriedade, String valor) {
        if (System.getProperty(propriedade) == null) System.setProperty(propriedade, valor);
    }

    public static String EnderecoWeb = System.getProperty("chat.web.bind", "127.0.0.1");
    public static int PortaWeb = Integer.getInteger("chat.web.port", 8081);
}
