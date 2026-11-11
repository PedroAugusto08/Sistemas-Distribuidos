package pacote;

import chat.swing.TemaSwing;
import java.awt.*;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import javax.swing.*;

public class FrmPainelDeControle extends JFrame {
    private final ServicoDesktop desktop = new ServicoDesktop(this::falhaDesktop);
    private final WebHttpService web = new WebHttpService();
    private final ColetorErrosHttp coletor = new ColetorErrosHttp();
    private LinhaServico linhaDesktop;
    private LinhaServico linhaWeb;
    private LinhaServico linhaColetor;
    private final JLabel aviso = TemaSwing.apoio("Os serviços podem ser ativados independentemente.");

    public FrmPainelDeControle() {
        TemaSwing.instalar();
        initComponents();
        SwingUtilities.updateComponentTreeUI(this);
        setMinimumSize(new Dimension(640, 480));
        setSize(720, 540);
        setLocationRelativeTo(null);
        // Falha/lentidão do TXT não impede usar o painel/coletor.
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception { Util.inicializarRepositorio(); return null; }
            @Override protected void done() {
                try { get(); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                catch (java.util.concurrent.ExecutionException e) {
                    aviso.setText("Histórico indisponível. Corrija o TXT para ativar o chat; coletor disponível.");
                }
            }
        }.execute();
    }

    // Estado e ação de cada listener no mesmo eixo; placeholders apenas no rodapé.
    // Paleta, superfícies, tipografia e ritmo seguem TemaSwing (G1).
    private JPanel criarConteudo() {
        JPanel pagina = TemaSwing.painel(new BorderLayout(0, 24), 24);
        JPanel topo = new JPanel(new BorderLayout(16, 0));
        JPanel titulos = new JPanel(new GridLayout(0, 1, 0, 8));
        titulos.add(TemaSwing.titulo("Middleware"));
        titulos.add(TemaSwing.apoio("CONTROLE DOS SERVIÇOS DO CHAT"));
        topo.add(titulos, BorderLayout.CENTER);
        topo.add(TemaSwing.alternador(), BorderLayout.EAST);
        pagina.add(topo, BorderLayout.NORTH);
        JPanel servicos = new JPanel(new GridLayout(3, 1, 0, 12));
        linhaDesktop = new LinhaServico("Desktop", desktop::isAtivo,
                () -> "TCP • recepção " + desktop.portaRecepcao() + " / envio " + desktop.portaEnvio(),
                desktop::iniciar, desktop::pararServidor, true);
        linhaWeb = new LinhaServico("Web", web::isAtivo,
                () -> "HTTP • porta " + web.portaLocal(), web::iniciar, web::pararServidor, true);
        linhaColetor = new LinhaServico("Coletor de erros", coletor::isAtivo,
                () -> coletor.isAtivo() ? "HTTP • porta " + coletor.portaLocal() :
                        (Integer.getInteger("chat.logs.port", 8082) == 0 ? "HTTP • porta automática" : "HTTP • porta configurada " + Integer.getInteger("chat.logs.port", 8082)),
                coletor::iniciar, coletor::pararServidor, false);
        servicos.add(linhaDesktop); servicos.add(linhaWeb); servicos.add(linhaColetor);
        pagina.add(servicos, BorderLayout.CENTER);
        JPanel rodape = new JPanel(new GridLayout(0, 1, 0, 8));
        rodape.add(TemaSwing.apoio("Terceiros e Publicidade • não implementados"));
        rodape.add(aviso);
        rodape.add(TemaSwing.apoio("Fechar o painel mantém os serviços e o acesso pela bandeja."));
        pagina.add(rodape, BorderLayout.SOUTH);
        return pagina;
    }

    private void falhaDesktop(Exception erro) {
        SwingUtilities.invokeLater(() -> {
            linhaDesktop.falha = "Falha";
            linhaDesktop.atualizar();
            aviso.setText("Desktop encerrado por falha. Consulte os logs e tente ativar novamente.");
        });
    }

    @FunctionalInterface private interface Operacao { void executar() throws Exception; }

    /** Um único padrão de estado e ação para os três serviços reais. */
    private final class LinhaServico extends JPanel {
        private final BooleanSupplier ativo;
        private final Supplier<String> portas;
        private final Operacao iniciar;
        private final Operacao parar;
        private final boolean confirmarParada;
        private final String nome;
        private final JLabel estado = new JLabel();
        private final JLabel endereco = TemaSwing.apoio("");
        private final JButton acao = new JButton();
        private String transicao;
        private String falha;

        LinhaServico(String nome, BooleanSupplier ativo, Supplier<String> portas,
                Operacao iniciar, Operacao parar, boolean confirmarParada) {
            super(new BorderLayout(24, 0));
            this.nome = nome; this.ativo = ativo; this.portas = portas;
            this.iniciar = iniciar; this.parar = parar; this.confirmarParada = confirmarParada;
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder(""), BorderFactory.createEmptyBorder(8, 16, 8, 16)));
            JPanel descricao = new JPanel(new GridLayout(0, 1, 0, 4));
            JLabel titulo = new JLabel(nome);
            titulo.setFont(new Font("Segoe UI", Font.BOLD, 18));
            descricao.add(titulo); descricao.add(endereco);
            add(descricao, BorderLayout.CENTER);
            JPanel controle = new JPanel(new BorderLayout(16, 0));
            controle.add(estado, BorderLayout.CENTER);
            TemaSwing.acao(acao);
            acao.addActionListener(e -> alterar());
            controle.add(acao, BorderLayout.EAST);
            add(controle, BorderLayout.EAST);
            atualizar();
        }

        void atualizar() {
            boolean ligado = ativo.getAsBoolean();
            estado.setText(transicao != null ? transicao : falha != null ? falha : ligado ? "Ativo" : "Parado");
            endereco.setText(portas.get());
            acao.setText(ligado ? "Desativar" : "Ativar");
            acao.setEnabled(transicao == null);
            acao.getAccessibleContext().setAccessibleName((ligado ? "Desativar " : "Ativar ") + nome);
        }

        private void alterar() {
            if (transicao != null) return;
            boolean ligar = !ativo.getAsBoolean();
            if (!ligar && confirmarParada && JOptionPane.showConfirmDialog(FrmPainelDeControle.this,
                    "Deseja parar o serviço " + nome + "?", "Confirmação", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
            falha = null;
            transicao = ligar ? "Ativando…" : "Desativando…";
            atualizar();
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() throws Exception {
                    if (ligar) iniciar.executar(); else parar.executar();
                    return null;
                }
                @Override protected void done() {
                    transicao = null;
                    try {
                        get();
                        if (ativo.getAsBoolean() != ligar) falha = "Falha";
                        aviso.setText(falha == null ? nome + (ligar ? " ativado." : " desativado.") : nome + " não iniciou. Consulte os logs.");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt(); falha = "Falha";
                    } catch (java.util.concurrent.ExecutionException e) {
                        falha = "Falha";
                        // Os serviços preservam o logging detalhado; a interface evita diálogos em cascata.
                        aviso.setText("Falha em " + nome + ". Verifique a porta e os logs antes de tentar novamente.");
                    }
                    atualizar();
                }
            }.execute();
        }
    }

    private void initComponents() {//GEN-BEGIN:initComponents
        conteudo = criarConteudo();
        setDefaultCloseOperation(javax.swing.WindowConstants.HIDE_ON_CLOSE);
        setTitle("Chat • Middleware");
        getContentPane().add(conteudo, java.awt.BorderLayout.CENTER);
        pack();
    }//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel conteudo;
    // End of variables declaration//GEN-END:variables

    public static void main(String[] args) { EventQueue.invokeLater(() -> new FrmPainelDeControle().setVisible(true)); }
}
