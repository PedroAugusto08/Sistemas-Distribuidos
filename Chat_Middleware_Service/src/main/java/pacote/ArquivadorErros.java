package pacote;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** Um proprietário por diretório/JVM. Filas em RAM; nenhuma E/S no chamador. */
public class ArquivadorErros implements AutoCloseable {
    private final Path pasta;
    private final Map<String, Destino> destinos = new HashMap<>();
    private final AtomicLong proximoAviso = new AtomicLong();
    private boolean fechado;

    public ArquivadorErros(Path pasta) { this(pasta, Set.of("desktop", "web"), Set.of("desktop", "web", "internal"), 128); }

    public ArquivadorErros(Path pasta, Set<String> clientes, Set<String> servicos, int capacidade) {
        this.pasta = pasta.toAbsolutePath().normalize();
        if (capacidade < 1) throw new IllegalArgumentException("Capacidade inválida.");
        // Valida todos antes de criar threads (inclusive cadastros de tipos futuros).
        for (String tipo : clientes) validarTipo(tipo);
        for (String tipo : servicos) validarTipo(tipo);
        clientes.forEach(tipo -> cadastrar("client_", tipo, capacidade));
        servicos.forEach(tipo -> cadastrar("server_middleware_", tipo, capacidade));
    }

    private void cadastrar(String familia, String tipo, int capacidade) {
        validarTipo(tipo);
        String nome = familia + tipo + "_errors.log";
        destinos.put(nome, new Destino(pasta.resolve(nome), capacidade));
    }

    private static void validarTipo(String tipo) {
        if (!tipo.matches("[a-z][a-z0-9_]{0,31}")) throw new IllegalArgumentException("Tipo inválido.");
    }

    /** Permite simular E/S bloqueada nos testes sem manipular arquivos reais. */
    protected void antesDeGravar(Path arquivo) throws IOException { }

    public synchronized boolean aceitaCliente(String tipo) {
        return destinos.containsKey("client_" + tipo + "_errors.log");
    }

    public CompletableFuture<Void> registrarCliente(String tipo, RegistroErro erro) {
        return enviar("client_" + tipo + "_errors.log", erro.formatar(null));
    }

    public CompletableFuture<Void> registrarMiddleware(String tipo, RegistroErro erro) {
        return enviar("server_middleware_" + tipo + "_errors.log", erro.formatar(tipo));
    }

    public CompletableFuture<Void> verificarCliente(String tipo) {
        return enviar("client_" + tipo + "_errors.log", null);
    }

    private synchronized CompletableFuture<Void> enviar(String nome, String texto) {
        Destino destino = destinos.get(nome);
        if (fechado || destino == null) return CompletableFuture.failedFuture(new IOException("Destino indisponível."));
        Pedido pedido = new Pedido(texto, new CompletableFuture<>());
        if (!destino.fila.offer(pedido)) {
            pedido.fim.completeExceptionally(new IOException("Fila de logs cheia."));
            avisar("Fila de logs cheia.");
        }
        return pedido.fim;
    }

    private void avisar(String mensagem) {
        long agora = System.nanoTime();
        long anterior = proximoAviso.get();
        if (agora >= anterior && proximoAviso.compareAndSet(anterior, agora + TimeUnit.SECONDS.toNanos(30))) {
            // Recurso final: nunca chama Util/HTTP ou tenta gravar outro log.
            System.err.println("Logging degradado: " + mensagem);
        }
    }

    private record Pedido(String texto, CompletableFuture<Void> fim) { }

    private final class Destino {
        final Path arquivo;
        final ArrayBlockingQueue<Pedido> fila;
        final Thread escritor;
        volatile boolean encerrando;
        boolean bloqueado;

        Destino(Path arquivo, int capacidade) {
            this.arquivo = arquivo;
            fila = new ArrayBlockingQueue<>(capacidade);
            // Uma thread dedicada por destino serializa a fila e as escritas em disco.
            escritor = Thread.ofPlatform().daemon().name("log-" + arquivo.getFileName()).start(this::executar);
        }

        void executar() {
            while (!encerrando || !fila.isEmpty()) {
                Pedido pedido;
                try { pedido = fila.poll(100, TimeUnit.MILLISECONDS); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
                if (pedido == null) continue;
                try {
                    gravar(pedido.texto);
                    pedido.fim.complete(null);
                } catch (IOException | RuntimeException e) {
                    pedido.fim.completeExceptionally(e);
                    avisar("Falha em " + arquivo.getFileName() + " (" + e.getClass().getSimpleName() + ").");
                }
            }
        }

        void gravar(String texto) throws IOException {
            if (bloqueado) throw new IOException("Destino bloqueado após escrita incompleta.");
            if (texto != null) antesDeGravar(arquivo);
            Files.createDirectories(pasta);
            if (Files.exists(arquivo, LinkOption.NOFOLLOW_LINKS)
                    && !Files.isRegularFile(arquivo, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Destino não é arquivo regular.");
            }
            try (FileChannel canal = FileChannel.open(arquivo, StandardOpenOption.CREATE,
                    StandardOpenOption.READ, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
                if (texto == null) return;
                long original = canal.size();
                // Examina apenas a terminação; preserva integralmente o conteúdo anterior.
                String separador = "";
                if (original > 0) {
                    ByteBuffer cauda = ByteBuffer.allocate((int) Math.min(4, original));
                    canal.position(original - cauda.capacity());
                    while (cauda.hasRemaining() && canal.read(cauda) != -1) { }
                    String fim = new String(cauda.array(), StandardCharsets.US_ASCII).replace("\r\n", "\n");
                    separador = fim.endsWith("\n\n") ? "" : fim.endsWith("\n") ? "\n" : "\n\n";
                }
                ByteBuffer dados = StandardCharsets.UTF_8.encode(separador + texto);
                canal.position(original);
                try {
                    while (dados.hasRemaining()) canal.write(dados);
                    canal.force(true);
                } catch (IOException | RuntimeException e) {
                    try { canal.truncate(original); canal.force(true); }
                    catch (IOException | RuntimeException rollback) { bloqueado = true; e.addSuppressed(rollback); }
                    throw e;
                }
            }
        }
    }

    @Override public void close() {
        synchronized (this) {
            fechado = true;
            destinos.values().forEach(d -> d.encerrando = true);
        }
        long fim = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        for (Destino destino : destinos.values()) {
            long restante = fim - System.nanoTime();
            if (restante <= 0) break;
            try { destino.escritor.join(Math.max(1, TimeUnit.NANOSECONDS.toMillis(restante))); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
        }
        if (destinos.values().stream().anyMatch(d -> d.escritor.isAlive())) avisar("Encerramento com escrita pendente.");
    }
}
