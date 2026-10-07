package pacote;

import javax.swing.JOptionPane;

public class frmLogin extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(frmLogin.class.getName());

    public frmLogin() {
        initComponents();
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        grpCor = new javax.swing.ButtonGroup();
        grpAvatar = new javax.swing.ButtonGroup();
        lblNick = new javax.swing.JLabel();
        txtnickname = new java.awt.TextField();
        lblCor = new javax.swing.JLabel();
        radioAzul = new javax.swing.JRadioButton();
        radioPreto = new javax.swing.JRadioButton();
        radioVermelho = new javax.swing.JRadioButton();
        lblAvatar = new javax.swing.JLabel();
        lblMenina = new javax.swing.JLabel();
        lblMenino = new javax.swing.JLabel();
        radMenina = new javax.swing.JRadioButton();
        radMenino = new javax.swing.JRadioButton();
        radAvatar1 = new javax.swing.JRadioButton();
        lblAzul = new javax.swing.JLabel();
        btnEntrar = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("LOGIN");
        setMinimumSize(new java.awt.Dimension(443, 353));
        setName("frmLogin"); // NOI18N
        getContentPane().setLayout(null);

        lblNick.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        lblNick.setText("Nickname:");
        getContentPane().add(lblNick);
        lblNick.setBounds(20, 30, 100, 30);

        txtnickname.addActionListener(this::txtnicknameActionPerformed);
        getContentPane().add(txtnickname);
        txtnickname.setBounds(130, 30, 200, 30);

        lblCor.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        lblCor.setText("Cor:");
        getContentPane().add(lblCor);
        lblCor.setBounds(20, 90, 40, 25);

        grpCor.add(radioAzul);
        radioAzul.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        radioAzul.setForeground(new java.awt.Color(0, 51, 255));
        radioAzul.setText("Azul");
        radioAzul.addActionListener(this::radioAzulActionPerformed);
        getContentPane().add(radioAzul);
        radioAzul.setBounds(90, 90, 90, 25);

        grpCor.add(radioPreto);
        radioPreto.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        radioPreto.setSelected(true);
        radioPreto.setText("Preto");
        radioPreto.addActionListener(this::radioPretoActionPerformed);
        getContentPane().add(radioPreto);
        radioPreto.setBounds(190, 80, 90, 50);

        grpCor.add(radioVermelho);
        radioVermelho.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        radioVermelho.setForeground(new java.awt.Color(255, 51, 51));
        radioVermelho.setText("Vermelho");
        radioVermelho.addActionListener(this::radioVermelhoActionPerformed);
        getContentPane().add(radioVermelho);
        radioVermelho.setBounds(300, 90, 103, 25);

        lblAvatar.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        lblAvatar.setText("Avatar:");
        getContentPane().add(lblAvatar);
        lblAvatar.setBounds(20, 140, 60, 25);

        lblMenina.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagens/menina.png")));
        lblMenina.setLabelFor(radMenina);
        getContentPane().add(lblMenina);
        lblMenina.setBounds(110, 130, 40, 40);

        lblMenino.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagens/menino.png")));
        lblMenino.setLabelFor(radMenino);
        getContentPane().add(lblMenino);
        lblMenino.setBounds(220, 130, 40, 40);

        grpAvatar.add(radMenina);
        radMenina.addActionListener(this::radMeninaActionPerformed);
        getContentPane().add(radMenina);
        radMenina.setBounds(90, 140, 19, 20);

        grpAvatar.add(radMenino);
        radMenino.setSelected(true);
        getContentPane().add(radMenino);
        radMenino.setBounds(190, 140, 20, 20);

        grpAvatar.add(radAvatar1);
        radAvatar1.addActionListener(this::radAvatar1ActionPerformed);
        getContentPane().add(radAvatar1);
        radAvatar1.setBounds(300, 140, 20, 20);

        lblAzul.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagens/avatar.png")));
        lblAzul.setLabelFor(radAvatar1);
        getContentPane().add(lblAzul);
        lblAzul.setBounds(330, 130, 60, 40);

        btnEntrar.setText("Entrar");
        btnEntrar.addActionListener(this::btnEntrarActionPerformed);
        getContentPane().add(btnEntrar);
        btnEntrar.setBounds(170, 240, 90, 30);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void txtnicknameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtnicknameActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtnicknameActionPerformed

    private void radioAzulActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioAzulActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_radioAzulActionPerformed

    private void radioPretoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioPretoActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_radioPretoActionPerformed

    private void radioVermelhoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radioVermelhoActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_radioVermelhoActionPerformed

    private void radMeninaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radMeninaActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_radMeninaActionPerformed

    private void radAvatar1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radAvatar1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_radAvatar1ActionPerformed

    private void btnEntrarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEntrarActionPerformed
        if(!txtnickname.getText().equals("")){
        
        Util.nickname = txtnickname.getText();
        
        if(radioAzul.isSelected()){
            Util.cor = "#0000FF";
        }else if(radioVermelho.isSelected()){
            Util.cor = "#FF0000";
        }else{
            Util.cor = "#000000";
        }
        
        if(radMenino.isSelected()){
            Util.avatar = "imagens/menino.png";
        }else if(radMenina.isSelected()){
            Util.avatar = "imagens/menina.png";
        }else if(radAvatar1.isSelected()){
            Util.avatar = "imagens/avatar.png";
        }
        
        frmChat frmchat = new frmChat();
        frmchat.setVisible(true);
        this.dispose();
        }else{
            JOptionPane.showMessageDialog(null, "Digite um nickname");
        }
    }//GEN-LAST:event_btnEntrarActionPerformed

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(() -> new frmLogin().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnEntrar;
    private javax.swing.ButtonGroup grpAvatar;
    private javax.swing.ButtonGroup grpCor;
    private javax.swing.JLabel lblAvatar;
    private javax.swing.JLabel lblAzul;
    private javax.swing.JLabel lblCor;
    private javax.swing.JLabel lblMenina;
    private javax.swing.JLabel lblMenino;
    private javax.swing.JLabel lblNick;
    private javax.swing.JRadioButton radAvatar1;
    private javax.swing.JRadioButton radMenina;
    private javax.swing.JRadioButton radMenino;
    private javax.swing.JRadioButton radioAzul;
    private javax.swing.JRadioButton radioPreto;
    private javax.swing.JRadioButton radioVermelho;
    private java.awt.TextField txtnickname;
    // End of variables declaration//GEN-END:variables
}
