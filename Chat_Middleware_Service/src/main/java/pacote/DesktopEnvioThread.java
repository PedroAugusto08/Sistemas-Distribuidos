package pacote;

import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.swing.JOptionPane;

public class DesktopEnvioThread implements Runnable {
    private Socket cliente;
    private ServerSocket emissor;

    @Override
    public void run() {
        try {
            emissor = new ServerSocket(Util.PortaEnvioDesktop);

            cliente = emissor.accept();

            ObjectOutputStream output = new ObjectOutputStream(cliente.getOutputStream());

            int mensagensEnviadas = 0;

            while(!cliente.isClosed()) {
                List<String> mensagens = Files.readAllLines(Path.of(Util.PathRepDesktop), StandardCharsets.UTF_8);

                while(mensagensEnviadas < mensagens.size()) {
                    String msg = mensagens.get(mensagensEnviadas);

                    output.writeUTF(msg);
                    output.flush();

                    mensagensEnviadas++;
                }

                Thread.sleep(200);
            }

            output.close();
            cliente.close();
            emissor.close();

        } catch(Exception e) {
            JOptionPane.showMessageDialog(null, "Erro em DesktopEnvioThread: " + e.getMessage());
            e.printStackTrace();
        }
    }
}