package pacote;

import java.awt.event.KeyEvent;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import javax.swing.JOptionPane;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

public class frmChat extends javax.swing.JFrame {
    public String msg = "";
    private final javax.swing.JPopupMenu popupEmoji = new javax.swing.JPopupMenu();
    private static final String MSG_MANUTENCAO = "<b><font color='red'>Em manutenção...</font></b><br>";
    
    public void gerarEnviarMensagem(){
        this.msg = "";
        this.msg += "<img src='" + Util.avatar + "' width='20' height='20'>";
        this.msg += "<font color='" + Util.cor + "'> " + Util.nickname + "</font>";

        if(cbModo.getSelectedItem().toString().equals("Fala")){
            this.msg += "<b> Fala: </b>";
            this.msg += txtMensagem.getText();
        }else if(cbModo.getSelectedItem().toString().equals("Grita")){
            this.msg += "<b><u> Grita: </u></b>";
            this.msg += "<font color='tomato' size='+1'>" + txtMensagem.getText().toUpperCase() + "</font>";
        }else if(cbModo.getSelectedItem().toString().equals("Xinga")){
            this.msg += "<b><i><u> Xinga: </u></i></b>";
            this.msg += "<font color='DarkRed' size='+2'>" + txtMensagem.getText().toUpperCase() + "</font>";
        }

        this.msg += "<br>";
        
        String[][] carinhas = {
            {":-)", "&#128578;"},  // 🙂
            {":)",  "&#128578;"},
            {";-)", "&#128521;"},  // 😉
            {";)",  "&#128521;"},
            {":-D", "&#128516;"},  // 😄
            {":D",  "&#128516;"},
            {"XD",  "&#128514;"},  // 😂
            {":-(", "&#128577;"},  // 🙁
            {":(",  "&#128577;"},
            {":'(", "&#128546;"},  // 😢
            {":-P", "&#128539;"},  // 😛
            {":P",  "&#128539;"},
            {":-/", "&#128533;"},  // 😕
            {":/",  "&#128533;"},
            {":-O", "&#128558;"},  // 😮
            {":O",  "&#128558;"},
            {"<3",  "&#10084;"}    // ❤
        };

        for(String[] carinha : carinhas){
            this.msg = this.msg.replace(carinha[0], carinha[1]);
        }
        
        try{
            Socket cliente = new Socket(Util.ipServidor, 6662);
            ObjectOutputStream output = new ObjectOutputStream(cliente.getOutputStream());
            
            output.writeUTF(this.msg);
            output.close();
            cliente.close();
            
            txtMensagem.setText("");
            txtMensagem.requestFocus();
            
        }catch(Exception e){
            JOptionPane.showMessageDialog(null, "Erro cliente ao enviar: " +e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(frmChat.class.getName());

    public frmChat() {
        initComponents();
        configurarPopupEmoji();

        HTMLDocument doc = (HTMLDocument) edtConversa.getDocument();
        doc.setBase(getClass().getResource("/"));

        Thread.ofVirtual().start(() -> {
            boolean reconectar = false;

            while(true){
                try(Socket cliente = new Socket(Util.ipServidor, 6661);
                    ObjectInputStream input = new ObjectInputStream(cliente.getInputStream())){

                    if(reconectar){
                        javax.swing.SwingUtilities.invokeLater(() -> {
                            try{
                                HTMLDocument documento = (HTMLDocument) edtConversa.getDocument();
                                documento.remove(0, documento.getLength());

                                btnEnviar.setEnabled(true);
                                txtMensagem.setEnabled(true);
                                btnEmoji.setEnabled(true);
                                cbModo.setEnabled(true);
                            }catch(Exception e){
                                e.printStackTrace();
                            }
                        });

                        reconectar = false;
                    }

                    while(true){
                        String msgs = input.readUTF();

                        javax.swing.SwingUtilities.invokeLater(() -> {
                            try{
                                HTMLDocument documento = (HTMLDocument) edtConversa.getDocument();
                                HTMLEditorKit kit = (HTMLEditorKit) edtConversa.getEditorKit();
                                kit.insertHTML(documento, documento.getLength(), msgs, 0, 0, null);
                            }catch(Exception e){
                                e.printStackTrace();
                            }
                        });

                        if(msgs.equals(MSG_MANUTENCAO)){
                            reconectar = true;

                            javax.swing.SwingUtilities.invokeLater(() -> {
                                btnEnviar.setEnabled(false);
                                txtMensagem.setEnabled(false);
                                btnEmoji.setEnabled(false);
                                cbModo.setEnabled(false);
                            });

                            break;
                        }
                    }

                }catch(Exception e){
                    if(!reconectar){
                        JOptionPane.showMessageDialog(null, "Erro ao receber mensagem: " + e.getClass().getSimpleName() + "\n" + e.getMessage());
                        e.printStackTrace();
                        break;
                    }
                }

                if(reconectar){
                    try{
                        Thread.sleep(1000);
                    }catch(InterruptedException e){
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        });
    }
    
    private void configurarPopupEmoji(){
        javax.swing.JTabbedPane abas = new javax.swing.JTabbedPane();

        javax.swing.JPanel emojis = new javax.swing.JPanel(new java.awt.GridLayout(3, 4, 5, 5));

        adicionarEmoji(emojis, "😁");
        adicionarEmoji(emojis, "😂");
        adicionarEmoji(emojis, "😉");
        adicionarEmoji(emojis, "😍");
        adicionarEmoji(emojis, "😢");
        adicionarEmoji(emojis, "😡");
        adicionarEmoji(emojis, "👍");
        adicionarEmoji(emojis, "👎");
        adicionarEmoji(emojis, "❤");
        adicionarEmoji(emojis, "💰");
        adicionarEmoji(emojis, "💋");
        adicionarEmoji(emojis, "🔥");

        javax.swing.JPanel stickers = new javax.swing.JPanel();
        stickers.add(new javax.swing.JLabel("Stickers em breve"));

        abas.addTab("Emojis", emojis);
        abas.addTab("Stickers", stickers);

        popupEmoji.add(abas);
    }

    private void adicionarEmoji(javax.swing.JPanel painel, String emoji){
        javax.swing.JButton botao = new javax.swing.JButton(emoji);

        botao.addActionListener(e -> {
            txtMensagem.replaceSelection(emoji);
            txtMensagem.requestFocus();
            popupEmoji.setVisible(false);
        });

        painel.add(botao);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenu2 = new javax.swing.JMenu();
        scrollConversa = new javax.swing.JScrollPane();
        edtConversa = new javax.swing.JEditorPane();
        lblMensagem = new javax.swing.JLabel();
        txtMensagem = new javax.swing.JTextField();
        cbModo = new javax.swing.JComboBox<>();
        lblModo = new javax.swing.JLabel();
        btnEnviar = new javax.swing.JButton();
        btnEmoji = new javax.swing.JButton();

        jMenu1.setText("File");
        jMenuBar1.add(jMenu1);

        jMenu2.setText("Edit");
        jMenuBar1.add(jMenu2);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("CHAT");
        setMinimumSize(new java.awt.Dimension(780, 550));
        setPreferredSize(new java.awt.Dimension(645, 514));
        getContentPane().setLayout(null);

        edtConversa.setEditable(false);
        edtConversa.setContentType("text/html"); // NOI18N
        scrollConversa.setViewportView(edtConversa);

        getContentPane().add(scrollConversa);
        scrollConversa.setBounds(23, 14, 720, 330);

        lblMensagem.setFont(new java.awt.Font("sansserif", 1, 16)); // NOI18N
        lblMensagem.setText("Mensagem");
        getContentPane().add(lblMensagem);
        lblMensagem.setBounds(30, 370, 100, 40);

        txtMensagem.setBackground(new java.awt.Color(204, 204, 204));
        txtMensagem.addActionListener(this::txtMensagemActionPerformed);
        txtMensagem.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txtMensagemKeyPressed(evt);
            }
        });
        getContentPane().add(txtMensagem);
        txtMensagem.setBounds(120, 370, 470, 40);

        cbModo.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        cbModo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Fala", "Grita", "Xinga" }));
        getContentPane().add(cbModo);
        cbModo.setBounds(120, 430, 90, 40);

        lblModo.setFont(new java.awt.Font("sansserif", 1, 18)); // NOI18N
        lblModo.setText("Modo");
        getContentPane().add(lblModo);
        lblModo.setBounds(40, 440, 70, 24);

        btnEnviar.setBackground(new java.awt.Color(0, 255, 0));
        btnEnviar.setFont(new java.awt.Font("sansserif", 1, 14)); // NOI18N
        btnEnviar.setText("➤");
        btnEnviar.addActionListener(this::btnEnviarActionPerformed);
        btnEnviar.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                btnEnviarKeyPressed(evt);
            }
        });
        getContentPane().add(btnEnviar);
        btnEnviar.setBounds(670, 370, 70, 40);

        btnEmoji.setBackground(new java.awt.Color(204, 204, 204));
        btnEmoji.setText("🙂");
        btnEmoji.addActionListener(this::btnEmojiActionPerformed);
        getContentPane().add(btnEmoji);
        btnEmoji.setBounds(610, 370, 50, 40);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void txtMensagemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtMensagemActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtMensagemActionPerformed

    private void btnEnviarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEnviarActionPerformed
        this.gerarEnviarMensagem();
    }//GEN-LAST:event_btnEnviarActionPerformed

    private void btnEnviarKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_btnEnviarKeyPressed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnEnviarKeyPressed

    private void txtMensagemKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtMensagemKeyPressed
        if(evt.getKeyCode() == KeyEvent.VK_ENTER){
            this.gerarEnviarMensagem();
        }
    }//GEN-LAST:event_txtMensagemKeyPressed

    private void btnEmojiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEmojiActionPerformed
        popupEmoji.show(btnEmoji, 0, btnEmoji.getHeight());
    }//GEN-LAST:event_btnEmojiActionPerformed

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(() -> new frmChat().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnEmoji;
    private javax.swing.JButton btnEnviar;
    private javax.swing.JComboBox<String> cbModo;
    private javax.swing.JEditorPane edtConversa;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JLabel lblMensagem;
    private javax.swing.JLabel lblModo;
    private javax.swing.JScrollPane scrollConversa;
    private javax.swing.JTextField txtMensagem;
    // End of variables declaration//GEN-END:variables
}
