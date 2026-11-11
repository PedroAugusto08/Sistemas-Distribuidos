package chat.swing;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.ColorUIResource;

/**
 * Estilo compartilhado pelos dois executáveis; apenas Swing/JDK, sem runtime externo.
 * Intenção: entrar, conversar e operar os listeners com a calma visual do Chat Web G1.
 * Paleta: grafite da sala, azul de ação, neutros para leitura e estados escritos por extenso.
 * Profundidade: superfícies #101419 / #181e26 / #222b36, sem sombras decorativas.
 * Tipografia: Segoe UI 14 regular, apoio 12, títulos 24 negrito; legível no DPI nativo.
 * Espaçamento: base 8, campos 44–48, interiores 16–32; histórico tem o espaço principal.
 */
public final class TemaSwing {
    private static boolean instalado;
    private static boolean claro;
    private TemaSwing() { }
    public static Color fundo() { return Color.decode(claro ? "#f3f5f7" : "#101419"); }
    public static Color superficie() { return Color.decode(claro ? "#ffffff" : "#181e26"); }
    public static Color elevado() { return Color.decode(claro ? "#e9eef4" : "#222b36"); }
    public static Color mensagemPropria() { return Color.decode(claro ? "#dceafa" : "#1b3048"); }
    public static Color texto() { return Color.decode(claro ? "#17212d" : "#e9eef5"); }
    public static Color secundario() { return Color.decode(claro ? "#526175" : "#adb8c7"); }
    public static Color azul() { return Color.decode(claro ? "#175cd3" : "#6aaeff"); }
    public static void instalar() {
        if (instalado) return;
        instalado = true;
        aplicar();
    }
    private static void aplicar() {
        try {
            // Recria os painters Nimbus para que o tema alcance também controles e popups.
            UIManager.put("control", new ColorUIResource(superficie()));
            UIManager.put("info", new ColorUIResource(elevado()));
            UIManager.put("nimbusBase", new ColorUIResource(elevado()));
            UIManager.put("nimbusBlueGrey", new ColorUIResource(elevado()));
            UIManager.put("nimbusLightBackground", new ColorUIResource(fundo()));
            UIManager.put("text", new ColorUIResource(texto()));
            UIManager.put("nimbusSelectionBackground", new ColorUIResource(Color.decode("#246bd1")));
            UIManager.put("nimbusSelectedText", new ColorUIResource(Color.WHITE));
            UIManager.put("nimbusFocus", new ColorUIResource(azul()));
            UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 14));
            UIManager.setLookAndFeel(new javax.swing.plaf.nimbus.NimbusLookAndFeel());
        } catch (UnsupportedLookAndFeelException e) {
            throw new IllegalStateException("Nimbus indisponível no JDK.", e);
        }
    }
    public static JButton alternador() {
        JButton b = new JButton("Tema claro / escuro");
        b.setToolTipText("Alternar tema de todas as janelas");
        b.addActionListener(e -> {
            claro = !claro;
            aplicar();
            for (Window w : Window.getWindows()) {
                SwingUtilities.updateComponentTreeUI(w);
            }
        });
        return b;
    }
    public static JPanel painel(LayoutManager layout, int margem) {
        JPanel p = new JPanel(layout);
        p.setBorder(BorderFactory.createEmptyBorder(margem, margem, margem, margem));
        return p;
    }
    public static JLabel titulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font("Segoe UI", Font.BOLD, 24));
        return l;
    }
    public static JLabel apoio(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        return l;
    }
    public static void acao(AbstractButton b) {
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setPreferredSize(new Dimension(Math.max(104, b.getPreferredSize().width), 44));
    }
}
