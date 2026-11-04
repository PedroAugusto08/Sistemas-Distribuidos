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
            repositorioMensagens = new RepositorioMensagens(Path.of(PathRepDesktop));
        }
    }

    public static synchronized RepositorioMensagens obterRepositorio() {
        if (repositorioMensagens == null) {
            throw new IllegalStateException("Histórico de mensagens não inicializado.");
        }
        return repositorioMensagens;
    }

    public static DesktopRecepcaoThread desktopRecepcaoThread;
    public static DesktopEnvioThread desktopEnvioThread;
}