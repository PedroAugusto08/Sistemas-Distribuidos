package pacote;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import javax.swing.JOptionPane;

public class DesktopRecepcaoThread implements Runnable {
    private volatile boolean ParadaManual = false;
    private Socket cliente;
    private ServerSocket receptor;

    @Override
    public void run() {
        try {
            receptor = new ServerSocket(Util.PortaRecepcaoDesktop);

            while (!ParadaManual) {
                cliente = receptor.accept();

                ObjectInputStream input = new ObjectInputStream(cliente.getInputStream());

                String msg = input.readUTF();

                input.close();
                
                cliente.close();
                cliente = null;

                try {
                    Util.obterRepositorio().adicionarMensagem(msg);
                } catch (IOException e) {
                    System.err.println("Erro ao persistir mensagem recebida: " + e.getMessage());
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            if (!ParadaManual || !(e instanceof SocketException)) {
                e.printStackTrace();
            }
            if (!ParadaManual) {
                JOptionPane.showMessageDialog(
                    null,
                    "Erro na Thread [Recepção Desktop]:\n" + e.getMessage()
                );
            }
        } finally {
            try {
                if (cliente != null && !cliente.isClosed()) {
                    cliente.close();
                }

                if (receptor != null && !receptor.isClosed()) {
                    receptor.close();
                }
            } catch (Exception e) {
            }
        }
    }

    public void pararServidor() {
        ParadaManual = true;

        try {
            if (cliente != null && !cliente.isClosed()) {
                cliente.close();
            }

            if (receptor != null && !receptor.isClosed()) {
                receptor.close();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                null,
                "Erro em DesktopRecepcaoThread :: pararServidor\n" + e.getMessage()
            );
        }
    }
}