package pacote;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class DesktopEnvioThread implements Runnable {
    private static final String MSG_MANUTENCAO = "<b><font color='red'>Em manutenção...</font></b><br>";
    private final ServerSocket emissor;
    private final RepositorioMensagens repositorio;
    private final Consumer<Exception> aoFalhar;
    private final ExecutorService tarefas = Executors.newVirtualThreadPerTaskExecutor();
    private final Set<Cliente> clientes = new HashSet<>();
    private volatile boolean paradaManual;
    private final CountDownLatch liberarConexoes = new CountDownLatch(1);

    private static class Cliente {
        final Socket socket;
        final ReentrantLock escrita = new ReentrantLock();
        volatile ObjectOutputStream output;
        volatile long inicioEscrita = System.nanoTime();

        Cliente(Socket socket) { this.socket = socket; }

        void enviar(String mensagem) throws IOException {
            escrita.lock();
            try {
                inicioEscrita = System.nanoTime();
                output.writeUTF(mensagem);
                output.flush();
            } finally {
                inicioEscrita = 0;
                escrita.unlock();
            }
        }

        void fechar() {
            try { socket.close(); } catch (IOException e) { /* Já encerrado. */ }
        }
    }

    public DesktopEnvioThread(int porta, RepositorioMensagens repositorio,
            Consumer<Exception> aoFalhar) throws IOException {
        this.repositorio = repositorio;
        this.aoFalhar = aoFalhar;
        emissor = new ServerSocket(porta);
    }

    @Override
    public void run() {
        try {
            tarefas.submit(this::vigiarEscritas);
            while (!paradaManual) {
                Cliente cliente = new Cliente(emissor.accept());
                synchronized (clientes) {
                    if (paradaManual || clientes.size() >= 128) cliente.fechar();
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

    private void atenderCliente(Cliente cliente) {
        Socket socket = cliente.socket;
        try {
            cliente.output = new ObjectOutputStream(socket.getOutputStream());
            cliente.output.flush();
            cliente.inicioEscrita = 0;
            tarefas.submit(() -> {
                try {
                    while (socket.getInputStream().read() != -1) { }
                } catch (IOException e) { /* Desconexão. */ }
                finally { cliente.fechar(); }
            });
            int indice = 0;
            while (!paradaManual && !socket.isClosed()) {
                for (String mensagem : repositorio.consultarMensagens(indice)) {
                    if (paradaManual || socket.isClosed()) break;
                    String exibicao = MensagemHtml.paraExibicao(mensagem);
                    long bytes = MensagemHtml.tamanhoUtfModificado(exibicao);
                    if (bytes > 65535) {
                        System.err.println("Mensagem histórica no índice " + indice + " possui " + bytes
                                + " bytes em UTF modificado após filtragem; enviado aviso ao Desktop.");
                        exibicao = "<b>Esta mensagem excede o limite de exibição do Desktop (65535 bytes). "
                                + "O conteúdo original permanece no histórico Web e no TXT.</b><br>";
                    }
                    cliente.enviar(exibicao);
                    indice++;
                }
                Thread.sleep(200);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            if (!paradaManual && !cliente.socket.isClosed()) {
                System.err.println("Conexão TCP encerrada: " + e.getClass().getSimpleName());
            }
        } finally {
            if (paradaManual) {
                try { liberarConexoes.await(1, TimeUnit.SECONDS); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
            cliente.fechar();
            synchronized (clientes) { clientes.remove(cliente); }
        }
    }

    private void vigiarEscritas() {
        try {
            while (!paradaManual) {
                synchronized (clientes) {
                    for (Cliente cliente : clientes) {
                        long inicio = cliente.inicioEscrita;
                        if (inicio != 0 && System.nanoTime() - inicio > TimeUnit.SECONDS.toNanos(5)) cliente.fechar();
                    }
                }
                Thread.sleep(200);
            }
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    public void pararServidor() {
        ArrayList<Cliente> atuais;
        synchronized (clientes) {
            paradaManual = true;
            try { emissor.close(); } catch (IOException e) { /* Já encerrado. */ }
            atuais = new ArrayList<>(clientes);
        }
        CountDownLatch avisos = new CountDownLatch(atuais.size());
        for (Cliente cliente : atuais) {
            // Melhor esforço: uma escrita lenta não pode impedir o fechamento.
            Thread.ofVirtual().start(() -> {
                try {
                    if (cliente.output != null) cliente.enviar(MSG_MANUTENCAO);
                } catch (IOException e) { /* Cliente indisponível. */ }
                finally { avisos.countDown(); }
            });
        }
        try { avisos.await(500, TimeUnit.MILLISECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        atuais.forEach(Cliente::fechar);
        liberarConexoes.countDown();
        tarefas.shutdownNow();
        try { tarefas.awaitTermination(3, TimeUnit.SECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
