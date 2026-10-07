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
    private volatile boolean ParadaManual = false;
    private Socket cliente;
    private ServerSocket emissor;

    @Override
    public void run() {
        ObjectOutputStream output = null;

        try {
            emissor = new ServerSocket(Util.PortaEnvioDesktop);

            if(ParadaManual){
                return;
            }

            cliente = emissor.accept();
            output = new ObjectOutputStream(cliente.getOutputStream());

            int mensagensEnviadas = 0;

            while(!ParadaManual && !cliente.isClosed()){
                List<String> mensagens = Files.readAllLines(Path.of(Util.PathRepDesktop), StandardCharsets.UTF_8);

                while(!ParadaManual && mensagensEnviadas < mensagens.size()){
                    String msg = mensagens.get(mensagensEnviadas);

                    output.writeUTF(msg);
                    output.flush();

                    mensagensEnviadas++;
                }

                Thread.sleep(200);
            }

        }catch(Exception e){
            if(!ParadaManual){
                JOptionPane.showMessageDialog(null, "Erro em DesktopEnvioThread: " + e.getMessage());
                e.printStackTrace();
            }
        }finally{
            if(output != null){
                try{
                    output.close();
                }catch(Exception e){
                }
            }

            fecharConexoes();
        }
    }

    public void pararServidor(){
        ParadaManual = true;
        fecharConexoes();
    }

    private void fecharConexoes(){
        try{
            if(cliente != null && !cliente.isClosed()){
                cliente.close();
            }
        }catch(Exception e){
            if(!ParadaManual){
                JOptionPane.showMessageDialog(null, "Erro ao fechar cliente Desktop: " + e.getMessage());
            }
        }

        try{
            if(emissor != null && !emissor.isClosed()){
                emissor.close();
            }
        }catch(Exception e){
            if(!ParadaManual){
                JOptionPane.showMessageDialog(null, "Erro ao fechar servidor de envio Desktop: " + e.getMessage());
            }
        }
    }
}