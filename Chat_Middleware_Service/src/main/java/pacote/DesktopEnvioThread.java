package pacote;

import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.JOptionPane;

public class DesktopEnvioThread implements Runnable {
    private static final String MSG_MANUTENCAO = "<b><font color='red'>Em manutenção...</font></b><br>";

    private volatile boolean ParadaManual = false;
    private volatile ServerSocket emissor;
    private final Map<Socket, ObjectOutputStream> clientes = new ConcurrentHashMap<>();

    @Override
    public void run(){
        try(ServerSocket servidor = new ServerSocket(Util.PortaEnvioDesktop)){
            emissor = servidor;

            while(!ParadaManual){
                try{
                    Socket cliente = servidor.accept();

                    if(ParadaManual){
                        cliente.close();
                        break;
                    }

                    Thread.ofVirtual().start(() -> atenderCliente(cliente));

                }catch(SocketException e){
                    if(!ParadaManual){
                        e.printStackTrace();
                    }
                    break;
                }
            }

        }catch(Exception e){
            if(!ParadaManual){
                JOptionPane.showMessageDialog(null, "Erro ao iniciar serviço Desktop: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void atenderCliente(Socket cliente){
        try(Socket socket = cliente;
            ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream())){

            clientes.put(socket, output);

            Thread.ofVirtual().start(() -> {
                try{
                    while(socket.getInputStream().read() != -1){
                    }
                }catch(Exception e){
                }finally{
                    fecharSocket(socket);
                }
            });

            int mensagensEnviadas = 0;

            while(!ParadaManual && !socket.isClosed()){
                List<String> mensagens = Files.readAllLines(Path.of(Util.PathRepDesktop), StandardCharsets.UTF_8);

                if(mensagensEnviadas > mensagens.size()){
                    mensagensEnviadas = 0;
                }

                while(!ParadaManual && !socket.isClosed() && mensagensEnviadas < mensagens.size()){
                    synchronized(output){
                        output.writeUTF(mensagens.get(mensagensEnviadas));
                        output.flush();
                    }
                    mensagensEnviadas++;
                }

                Thread.sleep(200);
            }

        }catch(InterruptedException e){
            Thread.currentThread().interrupt();
        }catch(Exception e){
            if(!ParadaManual && !cliente.isClosed()){
                System.out.println("Erro na conexão Desktop: " + e.getMessage());
            }
        }finally{
            clientes.remove(cliente);
            fecharSocket(cliente);
        }
    }

    public void pararServidor(){
        ParadaManual = true;

        for(Map.Entry<Socket, ObjectOutputStream> entrada : clientes.entrySet()){
            try{
                ObjectOutputStream output = entrada.getValue();

                synchronized(output){
                    output.writeUTF(MSG_MANUTENCAO);
                    output.flush();
                }
            }catch(Exception e){
            }
        }

        for(Socket cliente : clientes.keySet()){
            fecharSocket(cliente);
        }

        try{
            if(emissor != null && !emissor.isClosed()){
                emissor.close();
            }
        }catch(Exception e){
        }
    }

    private void fecharSocket(Socket cliente){
        try{
            if(cliente != null && !cliente.isClosed()){
                cliente.close();
            }
        }catch(Exception e){
        }
    }
}