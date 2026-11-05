package pacote;

import java.io.IOException;
import java.util.function.Consumer;

public class ServicoDesktop {
    private DesktopRecepcaoThread recepcao;
    private DesktopEnvioThread envio;
    private Thread threadRecepcao;
    private Thread threadEnvio;
    private volatile boolean ativo;
    private Object geracao;
    private final Consumer<Exception> aoFalhar;

    public ServicoDesktop(Consumer<Exception> aoFalhar) { this.aoFalhar = aoFalhar; }

    public synchronized void iniciar() throws IOException {
        if (ativo) return;
        Util.inicializarRepositorio();
        RepositorioMensagens repositorio = Util.obterRepositorio();
        Object atual = new Object();
        geracao = atual;
        try {
            recepcao = new DesktopRecepcaoThread(Util.PortaRecepcaoDesktop, repositorio, erro -> falhou(atual, erro));
            envio = new DesktopEnvioThread(Util.PortaEnvioDesktop, repositorio, erro -> falhou(atual, erro));
            ativo = true;
            threadRecepcao = Thread.ofVirtual().name("desktop-recepcao").start(recepcao);
            threadEnvio = Thread.ofVirtual().name("desktop-envio").start(envio);
        } catch (IOException | RuntimeException e) {
            pararServidor();
            Util.registrarErro("desktop", "inicializacao", "-", getClass().getName(), "iniciar", e);
            throw e;
        }
    }

    private void falhou(Object origem, Exception erro) {
        // O listener não pode aguardar o próprio término no encerramento.
        Thread.ofVirtual().start(() -> {
            synchronized (this) {
                if (geracao != origem) return;
                pararServidor();
                aoFalhar.accept(erro);
            }
        });
    }

    public synchronized void pararServidor() {
        ativo = false;
        geracao = null;
        if (recepcao != null) recepcao.pararServidor();
        if (envio != null) envio.pararServidor();
        aguardar(threadRecepcao);
        aguardar(threadEnvio);
        recepcao = null;
        envio = null;
        threadRecepcao = null;
        threadEnvio = null;
    }

    private void aguardar(Thread thread) {
        if (thread != null) {
            try { thread.join(3000); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    public boolean isAtivo() { return ativo; }
}
