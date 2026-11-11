package pacote;

import chat.swing.TemaSwing;
import java.awt.*;
import javax.swing.*;

public class frmLogin extends JFrame {
    private final JTextField txtnickname = new JTextField(24);
    private final JRadioButton radioAzul = new JRadioButton("Azul");
    private final JRadioButton radioPreto = new JRadioButton("Preto", true);
    private final JRadioButton radioVermelho = new JRadioButton("Vermelho");
    private final JRadioButton radMenina = new JRadioButton("Menina");
    private final JRadioButton radMenino = new JRadioButton("Menino", true);
    private final JRadioButton radAvatar1 = new JRadioButton("Avatar");
    private final JLabel[] imagensAvatar = {new JLabel("○", SwingConstants.CENTER), new JLabel("○", SwingConstants.CENTER), new JLabel("○", SwingConstants.CENTER)};
    private final JLabel aviso = TemaSwing.apoio("Seu perfil vale para esta conversa.");

    public frmLogin() {
        TemaSwing.instalar();
        initComponents();
        SwingUtilities.updateComponentTreeUI(this);
        setMinimumSize(new Dimension(480, 560));
        setSize(560, 620);
        setLocationRelativeTo(null);
        carregarAvataresRemotos();
    }

    // Conteúdo mantido em Java; o .form delega a criação a este mesmo método.
    // Apelido como foco de entrada; avatar e cor secundários, ação ao final.
    // Paleta, superfícies, tipografia e ritmo seguem TemaSwing (G1).
    private JPanel criarConteudo() {
        JPanel pagina = TemaSwing.painel(new BorderLayout(0, 24), 32);
        JPanel topo = new JPanel(new BorderLayout());
        topo.add(TemaSwing.apoio("CHAT / DESKTOP"), BorderLayout.WEST);
        topo.add(TemaSwing.alternador(), BorderLayout.EAST);
        pagina.add(topo, BorderLayout.NORTH);
        JPanel campos = new JPanel();
        campos.setLayout(new BoxLayout(campos, BoxLayout.Y_AXIS));
        campos.add(TemaSwing.titulo("Entre na conversa"));
        campos.add(Box.createVerticalStrut(8));
        campos.add(TemaSwing.apoio("Escolha como você quer aparecer no chat."));
        campos.add(Box.createVerticalStrut(32));
        campos.add(new JLabel("Apelido"));
        campos.add(Box.createVerticalStrut(8));
        txtnickname.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        txtnickname.setPreferredSize(new Dimension(320, 44));
        txtnickname.getAccessibleContext().setAccessibleName("Apelido");
        campos.add(txtnickname);
        campos.add(Box.createVerticalStrut(24));
        campos.add(new JLabel("Cor do nome"));
        JPanel cores = new JPanel(new GridLayout(1, 3, 8, 0));
        ButtonGroup grupoCor = new ButtonGroup();
        for (JRadioButton b : new JRadioButton[]{radioAzul, radioPreto, radioVermelho}) {
            grupoCor.add(b); cores.add(b);
        }
        cores.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        campos.add(cores);
        campos.add(Box.createVerticalStrut(24));
        campos.add(new JLabel("Avatar"));
        JPanel avatares = new JPanel(new GridLayout(1, 3, 8, 0));
        ButtonGroup grupoAvatar = new ButtonGroup();
        JRadioButton[] opcoesAvatar = {radMenina, radMenino, radAvatar1};
        for (int i = 0; i < opcoesAvatar.length; i++) {
            JRadioButton b = opcoesAvatar[i];
            grupoAvatar.add(b);
            JPanel escolha = new JPanel(new BorderLayout(0, 8));
            escolha.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));
            JLabel imagem = imagensAvatar[i];
            imagem.setLabelFor(b);
            imagem.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            imagem.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent e) { b.doClick(); }
            });
            b.setHorizontalAlignment(SwingConstants.CENTER);
            escolha.add(imagem, BorderLayout.CENTER);
            escolha.add(b, BorderLayout.SOUTH);
            avatares.add(escolha);
        }
        avatares.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        campos.add(avatares);
        campos.add(Box.createVerticalGlue());
        for (Component c : campos.getComponents()) if (c instanceof JComponent jc) jc.setAlignmentX(0);
        pagina.add(campos, BorderLayout.CENTER);
        JPanel rodape = new JPanel(new BorderLayout(0, 12));
        rodape.add(aviso, BorderLayout.NORTH);
        JButton entrar = new JButton("Entrar na conversa  →");
        TemaSwing.acao(entrar);
        entrar.addActionListener(e -> entrar());
        txtnickname.addActionListener(e -> entrar());
        getRootPane().setDefaultButton(entrar);
        rodape.add(entrar, BorderLayout.CENTER);
        pagina.add(rodape, BorderLayout.SOUTH);
        return pagina;
    }

    private void entrar() {
        String nome = txtnickname.getText().strip();
        if (nome.isBlank() || nome.codePoints().anyMatch(Character::isISOControl)) {
            aviso.setText("Informe um apelido sem caracteres de controle.");
            txtnickname.requestFocusInWindow();
            return;
        }
        Util.nickname = nome;
        Util.cor = radioAzul.isSelected() ? "#0000FF" : radioVermelho.isSelected() ? "#FF0000" : "#000000";
        Util.avatar = radMenina.isSelected() ? "imagens/menina.png" : radMenino.isSelected() ? "imagens/menino.png" : "imagens/avatar.png";
        new frmChat().setVisible(true);
        dispose();
    }

    private void carregarAvataresRemotos() {
        Thread.ofVirtual().start(() -> {
            try {
                String[] nomes = {"menina", "menino", "avatar"};
                for (int i = 0; i < nomes.length; i++) {
                    var conexao = java.net.URI.create(Util.urlFigurinhas + "/imagens/" + nomes[i] + ".png").toURL().openConnection();
                    conexao.setConnectTimeout(2000); conexao.setReadTimeout(3000);
                    byte[] bytes;
                    try (var in = conexao.getInputStream()) { bytes = in.readNBytes(1024 * 1024); }
                    ImageIcon fonte = new ImageIcon(bytes);
                    if (fonte.getIconWidth() <= 0) throw new java.io.IOException("Falha ao carregar os avatares.");
                    double fator = 32.0 / Math.max(fonte.getIconWidth(), fonte.getIconHeight());
                    ImageIcon icone = ImagemCatalogo.escalar(fonte.getImage(), Math.max(1, (int)(fonte.getIconWidth()*fator)), Math.max(1, (int)(fonte.getIconHeight()*fator)));
                    JLabel imagem = imagensAvatar[i];
                    SwingUtilities.invokeLater(() -> { imagem.setText(""); imagem.setIcon(icone); });
                }
            } catch (Exception e) {
                Util.registrarErro(getClass().getName(), "carregarAvataresRemotos", "avatares", e);
                SwingUtilities.invokeLater(() -> aviso.setText("Avatares indisponíveis; você ainda pode entrar."));
            }
        });
    }

    private void initComponents() {//GEN-BEGIN:initComponents
        conteudo = criarConteudo();
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Chat • Entrar");
        getContentPane().add(conteudo, java.awt.BorderLayout.CENTER);
        pack();
    }//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel conteudo;
    // End of variables declaration//GEN-END:variables

    public static void main(String[] args) { EventQueue.invokeLater(() -> new frmLogin().setVisible(true)); }
}
