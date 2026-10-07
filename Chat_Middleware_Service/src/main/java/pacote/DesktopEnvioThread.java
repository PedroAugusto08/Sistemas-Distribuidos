package pacote;

import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.swing.JOptionPane;

public class DesktopEnvioThread implements Runnable {
    private static final String MSG_MANUTENCAO = "<b><font color='red'>Em manutenção...</font></b><br>";

    private volatile boolean ParadaManual = false;
    private Socket cliente;
    private ServerSocket emissor;
    private volatile ObjectOutputStream output;

    @Override
    public void run() {
        try{
            emissor = new ServerSocket(Util.PortaEnvioDesktop);

            while(!ParadaManual){
                try{
                    cliente = emissor.accept();

                    if(ParadaManual){
                        break;
                    }

                    output = new ObjectOutputStream(cliente.getOutputStream());
                    int mensagensEnviadas = 0;
                    
                    monitorarDesconexao(cliente);

                    while(!ParadaManual && !cliente.isClosed()){
                        List<String> mensagens = Files.readAllLines(Path.of(Util.PathRepDesktop), StandardCharsets.UTF_8);

                        while(!ParadaManual && mensagensEnviadas < mensagens.size()){
                            enviarMensagem(mensagens.get(mensagensEnviadas));
                            mensagensEnviadas++;
                        }

                        Thread.sleep(200);
                    }

                }catch(SocketException e){
                    if(!ParadaManual){
                        System.out.println("Cliente Desktop desconectado. Aguardando nova conexão...");
                    }
                }catch(Exception e){
                    if(!ParadaManual){
                        JOptionPane.showMessageDialog(null, "Erro em DesktopEnvioThread: " + e.getMessage());
                        e.printStackTrace();
                    }
                }finally{
                    fecharCliente();
                }
            }

        }catch(Exception e){
            if(!ParadaManual){
                JOptionPane.showMessageDialog(null, "Erro ao iniciar serviço de envio Desktop: " + e.getMessage());
                e.printStackTrace();
            }
        }finally{
            fecharConexoes();
        }
    }

    private synchronized void enviarMensagem(String msg) throws Exception {
        if(output != null){
            output.writeUTF(msg);
            output.flush();
        }
    }
    
    

    public void pararServidor(){
        try{
            enviarMensagem(MSG_MANUTENCAO);
        }catch(Exception e){
        }

        ParadaManual = true;
        fecharConexoes();
    }

    private void fecharCliente(){
        try{
            if(output != null){
                output.close();
            }
        }catch(Exception e){
        }

        try{
            if(cliente != null && !cliente.isClosed()){
                cliente.close();
            }
        }catch(Exception e){
        }

        output = null;
        cliente = null;
    }

    private void fecharConexoes(){
        fecharCliente();

        try{
            if(emissor != null && !emissor.isClosed()){
                emissor.close();
            }
        }catch(Exception e){
        }
    }
    
    private void monitorarDesconexao(Socket clienteAtual){
        Thread.ofVirtual().start(() -> {
            try{
                while(!ParadaManual && clienteAtual.getInputStream().read() != -1){
                }
            }catch(Exception e){
            }finally{
                try{
                    if(!clienteAtual.isClosed()){
                        clienteAtual.close();
                    }
                }catch(Exception e){
                }
            }
        });
    }
}