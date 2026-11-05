package pacote;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class DesktopRecepcaoThread implements Runnable {
    private final ServerSocket receptor;
    private final RepositorioMensagens repositorio;
    private final Consumer<Exception> aoFalhar;
    private final ExecutorService tarefas = Executors.newVirtualThreadPerTaskExecutor();
    private final Set<Socket> clientes = new HashSet<>();
    private volatile boolean paradaManual;

    public DesktopRecepcaoThread(int porta, RepositorioMensagens repositorio,
            Consumer<Exception> aoFalhar) throws IOException {
        this.repositorio = repositorio;
        this.aoFalhar = aoFalhar;
        receptor = new ServerSocket(porta);
    }

    @Override
    public void run() {
        try {
            while (!paradaManual) {
                Socket cliente = receptor.accept();
                synchronized (clientes) {
                    if (paradaManual || clientes.size() >= 128) fechar(cliente);
                    else {
                        clientes.add(cliente);
                        tarefas.submit(() -> atenderCliente(cliente));
                    }
                }
            }
        } catch (Exception e) {
            if (!paradaManual) aoFalhar.accept(e);
        }
    }

    private void atenderCliente(Socket cliente) {
        Future<?> prazo = null;
        try {
            // Inclui cabeçalho ObjectInputStream e envio a conta-gotas.
            prazo = tarefas.submit(() -> {
                try {
                    Thread.sleep(5000);
                    fechar(cliente);
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
            cliente.setSoTimeout(3000);
            try (cliente; ObjectInputStream input = new ObjectInputStream(cliente.getInputStream())) {
                String mensagem = input.readUTF();
                prazo.cancel(true);
                MensagemHtml.validar(mensagem);
                repositorio.adicionarMensagem(mensagem);
            }
        } catch (Exception e) {
            if (!paradaManual) System.err.println("Mensagem TCP não recebida/persistida: " + e.getClass().getSimpleName());
        } finally {
            if (prazo != null) prazo.cancel(true);
            synchronized (clientes) { clientes.remove(cliente); }
            fechar(cliente);
        }
    }

    public void pararServidor() {
        synchronized (clientes) {
            paradaManual = true;
            try { receptor.close(); } catch (IOException e) { /* Já encerrado. */ }
            clientes.forEach(DesktopRecepcaoThread::fechar);
            // Fechar sockets encerra leituras; não interromper FileChannel/rollback.
            tarefas.shutdown();
        }
        try {
            if (!tarefas.awaitTermination(3, TimeUnit.SECONDS)) {
                System.err.println("Recepção TCP: persistência ainda em andamento após fechamento da rede.");
            }
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private static void fechar(Socket socket) {
        try { socket.close(); } catch (IOException e) { /* Fechamento idempotente. */ }
    }
}
