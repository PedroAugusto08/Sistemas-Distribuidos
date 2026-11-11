package pacote;

import java.awt.*;
import javax.swing.*;

public class menuServidor {
    private final FrmPainelDeControle painelControle;

    public menuServidor() {
        painelControle = new FrmPainelDeControle();
        if (!SystemTray.isSupported()) {
            painelControle.setVisible(true);
            return;
        }
        try {
            ImageIcon imagem = new ImageIcon(getClass().getResource("/imagens/icon.png"));
            TrayIcon icone = new TrayIcon(imagem.getImage(), "Servidor do Chat");
            icone.setImageAutoSize(true);
            PopupMenu popup = new PopupMenu();
            MenuItem abrir = new MenuItem("Abrir Painel de Controle");
            abrir.addActionListener(e -> abrirPainel());
            popup.add(abrir);
            icone.setPopupMenu(popup);
            icone.addActionListener(e -> abrirPainel());
            SystemTray.getSystemTray().add(icone);
        } catch (Exception e) {
            Util.registrarErro("internal", "bandeja", "-", getClass().getName(), "menuServidor", e);
            painelControle.setVisible(true);
            JOptionPane.showMessageDialog(painelControle, "Bandeja indisponível. O painel permanece acessível.");
        }
    }

    private void abrirPainel() {
        EventQueue.invokeLater(() -> {
            painelControle.setVisible(true);
            painelControle.setExtendedState(JFrame.NORMAL);
            painelControle.toFront();
        });
    }
}
