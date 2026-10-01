package pacote;

import java.awt.CheckboxMenuItem;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

public class menuServidor {
    SystemTray Tray;
    TrayIcon Icon;
    PopupMenu popup;
    MenuItem mnuPainelControle;
    CheckboxMenuItem mnuItDesktop, mnuItWeb, mnuItTerceiros, mnuItPublicidade;
    Menu mnuAcoes;
    FrmPainelDeControle painelControle;
    
    public menuServidor(){
        try{
            painelControle = new FrmPainelDeControle();
            if(!SystemTray.isSupported()){
                System.out.println("Sem suporte a SystemTray!");
                return;
            }else{
                Tray = SystemTray.getSystemTray();
                ImageIcon imgIcone = new ImageIcon("C:\\Users\\Pedro\\OneDrive\\Documentos\\NetBeansProjects\\Chat_Middleware_Service\\src\\main\\java\\pacote\\imgs\\icon.png", "servidor do chat");
                Icon = new TrayIcon(imgIcone.getImage());
                Icon.setImageAutoSize(true);
                
                popup = new PopupMenu();
                
                mnuPainelControle = new MenuItem("Abrir Painel de Controle");
                mnuAcoes = new Menu("Ações");
                mnuItDesktop = new CheckboxMenuItem("Servidor Desktop");
                mnuItWeb = new CheckboxMenuItem("Servidor Web");
                mnuItTerceiros = new CheckboxMenuItem("Servidor Terceiros");
                mnuItPublicidade = new CheckboxMenuItem("Servidor Publicidade");
                
                mnuPainelControle.addActionListener(
                        new ActionListener(){
                    public void actionPerformed(ActionEvent e) {
                        painelControle.setVisible(true);
                    }
                });
                
                popup.add(mnuPainelControle);
                popup.addSeparator();
                
                mnuAcoes.add(mnuItDesktop);
                mnuAcoes.add(mnuItWeb);
                mnuAcoes.add(mnuItTerceiros);
                popup.addSeparator();
                mnuAcoes.add(mnuItPublicidade);
                
                popup.add(mnuAcoes);
                Icon.setPopupMenu(popup);
                Tray.add(Icon);
            }
        }catch(Exception e){
            JOptionPane.showMessageDialog(null, "Erro ao criar menu do servidor de chat: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
