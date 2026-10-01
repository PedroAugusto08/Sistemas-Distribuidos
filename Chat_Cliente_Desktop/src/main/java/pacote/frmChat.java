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
    
    public void gerarEnviarMensagem(){
        this.msg = "";
        this.msg += "<img src = '" + Util.avatar +"' width='20' height='20'>";
        this.msg += "<font color = '" + Util.cor +"'>";
        this.msg += "<font color ='cor'> apelido <font>";
        this.msg = this.msg.replace("cor", Util.cor);
        this.msg = this.msg.replace("apelido", Util.nickname);
        
        if(cbModo.getSelectedItem().toString().equals("Fala")){
            this.msg += "<b> Fala: </b>";
            this.msg += txtMensagem.getText();
        }else if(cbModo.getSelectedItem().toString().equals("Grita")){
            this.msg += "<b><u> Grita: </b></u>";
            this.msg += "<font color = 'tomato' size ='+1'>" + txtMensagem.getText().toUpperCase();
        }else if(cbModo.getSelectedItem().toString().equals("Xinga")){
            this.msg += "<b><i><u> Grita: </b></i></u>";
            this.msg += "<font color = 'DarkRed' size ='+2'>" + txtMensagem.getText().toUpperCase();
        }
        
        this.msg += "<br>";
        
        ArrayList <String> codigos = new ArrayList<String>();
        ArrayList <String> simbolos = new ArrayList<String>();
        
        codigos.add(":-)");
        simbolos.add("&#128513;");
        
        codigos.add(";-)");
        simbolos.add("&#128521;");
        
        codigos.add("LOL");
        simbolos.add("&#128514;");
        
        codigos.add(":<)");
        simbolos.add("&#128511;");
        
        codigos.add(":/");
        simbolos.add("&#128533;");
        
        codigos.add(">3");
        simbolos.add("&#128525;");
        
        for(int i=0; i<codigos.size(); i++){
            this.msg = this.msg.replace(codigos.get(i), simbolos.get(i));
        }
        
        try{
            Socket cliente = new Socket("200.128.141.210", 6662);
            ObjectOutputStream output = new ObjectOutputStream(cliente.getOutputStream());
            
            output.writeUTF(this.msg);
            output.close();
            cliente.close();
            
        }catch(Exception e){
            JOptionPane.showMessageDialog(null, "Erro cliente ao enviar: " +e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(frmChat.class.getName());

    public frmChat() {
        initComponents();

        try {
            HTMLDocument doc = (HTMLDocument) edtConversa.getDocument();
            java.io.File pastaBase = new java.io.File("src/main/java/");
            doc.setBase(pastaBase.toURI().toURL());

            System.out.println("Base HTML: " + doc.getBase());
            System.out.println("Imagem existe: " + new java.io.File("src/main/java/imagens/menino.png").exists());

        } catch(Exception e) {
            e.printStackTrace();
        }

        Thread.ofVirtual().start(() -> {
            try {
                Socket cliente = new Socket("200.128.141.210", 6661);

                ObjectInputStream input = new ObjectInputStream(cliente.getInputStream());

                while(true) {
                    String msgs = input.readUTF();

                    javax.swing.SwingUtilities.invokeLater(() -> {
                        try {
                            HTMLDocument doc = (HTMLDocument) edtConversa.getDocument();

                            HTMLEditorKit kit = (HTMLEditorKit) edtConversa.getEditorKit();

                            kit.insertHTML(doc, doc.getLength(), msgs, 0, 0, null);

                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    });
                }

            } catch(Exception e) {
                JOptionPane.showMessageDialog(
                        null,
                        "Erro ao receber mensagem: " + e.getMessage()
                );
            }
        });
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        scrollConversa = new javax.swing.JScrollPane();
        edtConversa = new javax.swing.JEditorPane();
        lblMensagem = new javax.swing.JLabel();
        txtMensagem = new javax.swing.JTextField();
        cbModo = new javax.swing.JComboBox<>();
        lblModo = new javax.swing.JLabel();
        lblEmoji = new javax.swing.JLabel();
        cbEmoji = new javax.swing.JComboBox<>();
        btnEnviar = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("CHAT");
        setMinimumSize(new java.awt.Dimension(488, 439));

        edtConversa.setEditable(false);
        edtConversa.setContentType("text/html"); // NOI18N
        scrollConversa.setViewportView(edtConversa);

        lblMensagem.setFont(new java.awt.Font("sansserif", 1, 14)); // NOI18N
        lblMensagem.setText("Mensagem");

        txtMensagem.addActionListener(this::txtMensagemActionPerformed);
        txtMensagem.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txtMensagemKeyPressed(evt);
            }
        });

        cbModo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Fala", "Grita", "Xinga" }));

        lblModo.setFont(new java.awt.Font("sansserif", 1, 14)); // NOI18N
        lblModo.setText("Modo");

        lblEmoji.setFont(new java.awt.Font("sansserif", 1, 14)); // NOI18N
        lblEmoji.setText("Emoji");

        cbEmoji.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Nenhum", "Coração", "Dinheiro", "Beijo" }));

        btnEnviar.setFont(new java.awt.Font("sansserif", 1, 14)); // NOI18N
        btnEnviar.setText("Enviar");
        btnEnviar.addActionListener(this::btnEnviarActionPerformed);
        btnEnviar.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                btnEnviarKeyPressed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(17, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(lblModo, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(lblEmoji))
                                .addGap(58, 58, 58)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(cbEmoji, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(cbModo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnEnviar, javax.swing.GroupLayout.PREFERRED_SIZE, 95, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(91, 91, 91))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(lblMensagem, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(txtMensagem, javax.swing.GroupLayout.PREFERRED_SIZE, 320, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(46, Short.MAX_VALUE))))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(scrollConversa, javax.swing.GroupLayout.PREFERRED_SIZE, 454, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(15, 15, 15))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addComponent(scrollConversa, javax.swing.GroupLayout.PREFERRED_SIZE, 276, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtMensagem, javax.swing.GroupLayout.DEFAULT_SIZE, 28, Short.MAX_VALUE)
                    .addComponent(lblMensagem))
                .addGap(7, 7, 7)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnEnviar, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cbModo, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblModo))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cbEmoji, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblEmoji))
                .addGap(21, 21, 21))
        );

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

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(() -> new frmChat().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnEnviar;
    private javax.swing.JComboBox<String> cbEmoji;
    private javax.swing.JComboBox<String> cbModo;
    private javax.swing.JEditorPane edtConversa;
    private javax.swing.JLabel lblEmoji;
    private javax.swing.JLabel lblMensagem;
    private javax.swing.JLabel lblModo;
    private javax.swing.JScrollPane scrollConversa;
    private javax.swing.JTextField txtMensagem;
    // End of variables declaration//GEN-END:variables
}
