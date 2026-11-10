package chat.catalogo;

import java.io.IOException;
import java.util.function.LongSupplier;

/** Cache curto de compatibilidade, sem autorizar com resultado expirado ou falho. */
public final class VerificacaoPublicacao {
    private final Catalogo catalogo;
    private final String base;
    private final LongSupplier relogio;
    private long proxima;
    private boolean conferido;
    private IOException falha;

    public VerificacaoPublicacao(Catalogo catalogo, String base) {
        this(catalogo, base, System::nanoTime);
    }

    // Relógio injetável para testar expiração sem aguardar tempo real.
    public VerificacaoPublicacao(Catalogo catalogo, String base, LongSupplier relogio) {
        this.catalogo = java.util.Objects.requireNonNull(catalogo);
        this.base = base;
        this.relogio = relogio;
    }

    public synchronized void conferir() throws IOException {
        long agora = relogio.getAsLong();
        if (!conferido || agora - proxima >= 0) {
            try { catalogo.conferirPublicacao(base); falha = null; }
            catch (IOException e) { falha = e; }
            conferido = true;
            proxima = relogio.getAsLong() + java.util.concurrent.TimeUnit.SECONDS.toNanos(falha == null ? 30 : 5);
        }
        if (falha != null) throw new IOException("Publicação ausente ou incompatível; nova tentativa em até 5 segundos.", falha);
    }
}
