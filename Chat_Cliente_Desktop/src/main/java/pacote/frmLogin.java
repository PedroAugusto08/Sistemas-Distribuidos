package pacote;

import javax.swing.JOptionPane;

public class frmLogin extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(frmLogin.class.getName());

    public frmLogin() {
        initComponents();
        configurarSelecaoAvatares();
    }
    
    private void configurarSelecaoAvatares(){
        lblMenina.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        lblMenino.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        lblAzul.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        lblMenina.addMouseListener(new java.awt.event.MouseAdapter(){
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt){
                radMenina.setSelected(true);
                atualizarBordasAvatares();
            }
        });

        lblMenino.addMouseListener(new java.awt.event.MouseAdapter(){
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt){
                radMenino.setSelected(true);
                atualizarBordasAvatares();
            }
        });

        lblAzul.addMouseListener(new java.awt.event.MouseAdapter(){
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt){
                radAvatar1.setSelected(true);
                atualizarBordasAvatares();
            }
        });

        radMenina.addActionListener(e -> atualizarBordasAvatares());
        radMenino.addActionListener(e -> atualizarBordasAvatares());
        radAvatar1.addActionListener(e -> atualizarBordasAvatares());

        atualizarBordasAvatares();
    }
    
    private void atualizarBordasAvatares(){
        javax.swing.border.Border normal = javax.swing.BorderFactory.createEmptyBorder(3, 3, 3, 3);
        javax.swing.border.Border selecionado = javax.swing.BorderFactory.createLineBorder(new java.awt.Color(37, 99, 235), 3);

        lblMenina.setBorder(radMenina.isSelected() ? selecionado : normal);
        lblMenino.setBorder(radMenino.isSelected() ? selecionado : normal);
        lblAzul.setBorder(radAvatar1.isSelected() ? selecionado : normal);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        grpCor = new javax.swing.ButtonGroup();
        grpAvatar = new javax.swing.ButtonGroup();
        lblNick = new javax.swing.JLabel();
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
        lblTitulo = new javax.swing.JLabel();
        lblSubtitulo = new javax.swing.JLabel();
        txtnickname = new javax.swing.JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("LOGIN");
        setMinimumSize(new java.awt.Dimension(440, 480));
        setName("frmLogin"); // NOI18N
        getContentPane().setLayout(null);

        lblNick.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        lblNick.setText("Nickname");
        getContentPane().add(lblNick);
        lblNick.setBounds(30, 70, 100, 30);

        lblCor.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        lblCor.setText("Escolha sua cor");
        getContentPane().add(lblCor);
        lblCor.setBounds(30, 150, 140, 25);

        grpCor.add(radioAzul);
        radioAzul.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        radioAzul.setForeground(new java.awt.Color(0, 51, 255));
        radioAzul.setText("Azul");
        radioAzul.setMaximumSize(new java.awt.Dimension(100, 100));
        radioAzul.setMinimumSize(new java.awt.Dimension(100, 100));
        radioAzul.addActionListener(this::radioAzulActionPerformed);
        getContentPane().add(radioAzul);
        radioAzul.setBounds(40, 180, 90, 25);

        grpCor.add(radioPreto);
        radioPreto.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        radioPreto.setSelected(true);
        radioPreto.setText("Preto");
        radioPreto.setMaximumSize(new java.awt.Dimension(100, 100));
        radioPreto.setMinimumSize(new java.awt.Dimension(100, 100));
        radioPreto.addActionListener(this::radioPretoActionPerformed);
        getContentPane().add(radioPreto);
        radioPreto.setBounds(190, 170, 90, 50);

        grpCor.add(radioVermelho);
        radioVermelho.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        radioVermelho.setForeground(new java.awt.Color(255, 51, 51));
        radioVermelho.setText("Vermelho");
        radioVermelho.setMaximumSize(new java.awt.Dimension(100, 100));
        radioVermelho.setMinimumSize(new java.awt.Dimension(100, 100));
        radioVermelho.addActionListener(this::radioVermelhoActionPerformed);
        getContentPane().add(radioVermelho);
        radioVermelho.setBounds(310, 180, 103, 30);

        lblAvatar.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        lblAvatar.setText("Escolha seu avatar");
        getContentPane().add(lblAvatar);
        lblAvatar.setBounds(30, 240, 160, 25);

        lblMenina.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagens/menina.png")));
        lblMenina.setLabelFor(radMenina);
        getContentPane().add(lblMenina);
        lblMenina.setBounds(40, 270, 40, 40);

        lblMenino.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagens/menino.png")));
        lblMenino.setLabelFor(radMenino);
        getContentPane().add(lblMenino);
        lblMenino.setBounds(190, 270, 40, 40);

        grpAvatar.add(radMenina);
        radMenina.addActionListener(this::radMeninaActionPerformed);
        getContentPane().add(radMenina);
        radMenina.setBounds(50, 310, 19, 20);

        grpAvatar.add(radMenino);
        radMenino.setSelected(true);
        radMenino.addActionListener(this::radMeninoActionPerformed);
        getContentPane().add(radMenino);
        radMenino.setBounds(200, 310, 20, 20);

        grpAvatar.add(radAvatar1);
        radAvatar1.addActionListener(this::radAvatar1ActionPerformed);
        getContentPane().add(radAvatar1);
        radAvatar1.setBounds(340, 310, 20, 20);

        lblAzul.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagens/avatar.png")));
        lblAzul.setLabelFor(radAvatar1);
        getContentPane().add(lblAzul);
        lblAzul.setBounds(330, 270, 40, 40);

        btnEntrar.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnEntrar.setText("Entrar no chat");
        btnEntrar.setMaximumSize(new java.awt.Dimension(300, 100));
        btnEntrar.setMinimumSize(new java.awt.Dimension(200, 42));
        btnEntrar.setPreferredSize(new java.awt.Dimension(200, 42));
        btnEntrar.addActionListener(this::btnEntrarActionPerformed);
        getContentPane().add(btnEntrar);
        btnEntrar.setBounds(140, 380, 140, 40);

        lblTitulo.setFont(new java.awt.Font("SansSerif", 1, 20)); // NOI18N
        lblTitulo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblTitulo.setText("Bem-vindo ao Chat");
        getContentPane().add(lblTitulo);
        lblTitulo.setBounds(110, 10, 220, 16);

        lblSubtitulo.setFont(new java.awt.Font("SansSerif", 0, 14)); // NOI18N
        lblSubtitulo.setForeground(new java.awt.Color(51, 51, 51));
        lblSubtitulo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblSubtitulo.setText("Personalize seu perfil para entrar");
        getContentPane().add(lblSubtitulo);
        lblSubtitulo.setBounds(100, 30, 240, 19);

        txtnickname.setToolTipText("Digite seu apelido...");
        txtnickname.addActionListener(this::txtnicknameActionPerformed);
        getContentPane().add(txtnickname);
        txtnickname.setBounds(30, 100, 360, 22);

        pack();
    }// </editor-fold>//GEN-END:initComponents

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

    private void radMeninoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radMeninoActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_radMeninoActionPerformed

    private void txtnicknameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtnicknameActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtnicknameActionPerformed

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
    private javax.swing.JLabel lblSubtitulo;
    private javax.swing.JLabel lblTitulo;
    private javax.swing.JRadioButton radAvatar1;
    private javax.swing.JRadioButton radMenina;
    private javax.swing.JRadioButton radMenino;
    private javax.swing.JRadioButton radioAzul;
    private javax.swing.JRadioButton radioPreto;
    private javax.swing.JRadioButton radioVermelho;
    private javax.swing.JTextField txtnickname;
    // End of variables declaration//GEN-END:variables
}
