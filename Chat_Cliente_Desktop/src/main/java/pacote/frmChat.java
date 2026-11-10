package pacote;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import javax.swing.JOptionPane;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

public class frmChat extends javax.swing.JFrame {
    public String msg = "";
    private chat.catalogo.Catalogo.Recurso imagemPendente;
    private final javax.swing.JLabel previa = new javax.swing.JLabel("Nenhuma imagem selecionada");
    private final javax.swing.JButton removerImagem = new javax.swing.JButton("Remover");
    private String ultimaTentativa;
    private final javax.swing.JLabel estadoEnvio = new javax.swing.JLabel(" ");
    private chat.catalogo.VerificacaoPublicacao verificacaoCatalogo;
    private javax.swing.Timer timerCatalogo;
    private final java.util.concurrent.atomic.AtomicBoolean verificandoCatalogo = new java.util.concurrent.atomic.AtomicBoolean();

    private volatile boolean encerrado;
    private volatile boolean conectado;
    private boolean envioEmAndamento;
    private volatile Socket conexaoRecepcao;
    private final javax.swing.JPopupMenu popupEmoji = new javax.swing.JPopupMenu();
    private static final String MSG_MANUTENCAO = "<b><font color='red'>Em manutenção...</font></b><br>";
    
    public void gerarEnviarMensagem(){
        // Enter enquanto navega no seletor não deve acionar o botão padrão da janela.
        if (popupEmoji.isVisible()) return;
        if (imagemPendente != null && previa.getIcon() == null) return;
        if (imagemPendente != null && imagemPendente.figurinha()) {
            enviarSticker(imagemPendente);
            return;
        }
        if (txtMensagem.getText().isBlank() && imagemPendente == null) return;

        String textoBase = txtMensagem.getText();
        if (!cbModo.getSelectedItem().toString().equals("Fala")) textoBase = textoBase.toUpperCase(java.util.Locale.ROOT);
        String textoMensagem = Util.escaparHtml(textoBase);

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
            {":&#39;(", "&#128546;"},  // 😢
            {":-P", "&#128539;"},  // 😛
            {":P",  "&#128539;"},
            {":-/", "&#128533;"},  // 😕
            {":/",  "&#128533;"},
            {":-O", "&#128558;"},  // 😮
            {":O",  "&#128558;"},
            {"&lt;3",  "&#10084;"}    // ❤
        };

        for(String[] carinha : carinhas){
            textoMensagem = textoMensagem.replace(carinha[0], carinha[1]);
        }

        this.msg = "";
        this.msg += "<img src='/figurinhas/" + Util.avatar + "' width='20' height='20'>";
        this.msg += "<font color='" + Util.cor + "'> " + Util.escaparHtml(Util.nickname) + "</font>";

        if(cbModo.getSelectedItem().toString().equals("Fala")){
            this.msg += "<b> Fala: </b>";
            this.msg += textoMensagem;
        }else if(cbModo.getSelectedItem().toString().equals("Grita")){
            this.msg += "<b><u> Grita: </u></b>";
            this.msg += "<font color='tomato' size='+1'>" + textoMensagem + "</font>";
        }else if(cbModo.getSelectedItem().toString().equals("Xinga")){
            this.msg += "<b><i><u> Xinga: </u></i></b>";
            this.msg += "<font color='DarkRed' size='+2'>" + textoMensagem + "</font>";
        }

        if (imagemPendente != null) this.msg += imagemPendente.imagemHtml();
        this.msg += "<br>";

        enviarMensagem(this.msg);
    }

    private void enviarMensagem(String mensagem) {
        if (envioEmAndamento || !conectado || encerrado) return;
        try { chat.catalogo.Catalogo.validarMensagem(mensagem); }
        catch (IllegalArgumentException e) { JOptionPane.showMessageDialog(this, e.getMessage()); return; }
        envioEmAndamento = true;
        if (mensagem.equals(ultimaTentativa) && JOptionPane.showConfirmDialog(this,
                "O protocolo não confirma persistência. Confira o histórico. Deseja reenviar esta mesma composição?",
                "Possível duplicação", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
            envioEmAndamento = false;
            return;
        }
        boolean possuiImagem = imagemPendente != null;
        habilitarEnvio(conectado);
        Thread.ofVirtual().start(() -> {
            Socket cliente = new Socket();
            Thread prazo = Thread.ofVirtual().start(() -> {
                try { Thread.sleep(5000); cliente.close(); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                catch (java.io.IOException e) { /* Já encerrado. */ }
            });
            try (cliente) {
                if (possuiImagem) {
                    if (verificacaoCatalogo == null) throw new java.io.IOException("Catálogo embarcado inválido.");
                    verificacaoCatalogo.conferir();
                }
                // A partir da conexão, qualquer falha pode deixar o resultado incerto.
                javax.swing.SwingUtilities.invokeAndWait(() -> ultimaTentativa = mensagem);
                cliente.connect(new java.net.InetSocketAddress(Util.ipServidor, Integer.getInteger("chat.desktop.envio.port", 6662)), 2000);
                try (ObjectOutputStream output = new ObjectOutputStream(cliente.getOutputStream())) {
                    output.writeUTF(mensagem);
                    output.flush();
                }
                javax.swing.SwingUtilities.invokeLater(() -> estadoEnvio.setText(
                        "Transmitido ao socket; persistência não confirmada. Composição preservada."));
            } catch (Exception e) {
                if (!encerrado) Util.registrarErro(getClass().getName(), "enviarMensagem", "envio", e);
                if (!encerrado) javax.swing.SwingUtilities.invokeLater(() -> estadoEnvio.setText(
                        "Falha de envio; confira o histórico antes de reenviar. Composição preservada."));
            } finally {
                prazo.interrupt();
                javax.swing.SwingUtilities.invokeLater(() -> {
                    envioEmAndamento = false;
                    habilitarEnvio(conectado);
                });
            }
        });
    }

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(frmChat.class.getName());

    public frmChat() {
        initComponents();
      
        javax.swing.SwingUtilities.invokeLater(() -> txtMensagem.requestFocusInWindow());
        getRootPane().setDefaultButton(btnEnviar);
        configurarPrevia();
        configurarPopupEmoji();
        habilitarEnvio(false);

        HTMLDocument doc = (HTMLDocument) edtConversa.getDocument();
        try{
            // Resolve as imagens do HTML usando o endereço do servidor Web.
            doc.setBase(java.net.URI.create(Util.urlFigurinhas + "/").toURL());
        }catch(Exception e){
            Util.registrarErro(getClass().getName(), "frmChat", "configuracao-figurinhas", e);
            throw new IllegalStateException("Endereço inválido do servidor de figurinhas.", e);
        }

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent evento) {
                encerrado = true;
                if (timerCatalogo != null) timerCatalogo.stop();
                Thread.ofVirtual().start(() -> {
                    Socket socket = conexaoRecepcao;
                    if (socket != null) {
                        try { socket.close(); } catch (java.io.IOException e) { /* Já encerrado. */ }
                    }
                });
            }
        });
        Thread.ofVirtual().start(this::receberMensagens);
    }

    private void receberMensagens() {
        boolean avisoIndisponivel = false;
        boolean manutencao = false;
        while (!encerrado) {
            try (Socket cliente = new Socket()) {
                conexaoRecepcao = cliente;
                cliente.connect(new java.net.InetSocketAddress(Util.ipServidor, Integer.getInteger("chat.desktop.recepcao.port", 6661)), 2000);
                cliente.setSoTimeout(5000);
                try (ObjectInputStream input = new ObjectInputStream(cliente.getInputStream())) {
                    cliente.setSoTimeout(0);
                    conectado = true;
                    manutencao = false;
                    RelatorErrosDesktop.padrao().recuperar(getClass().getName(), "receberMensagens", "recepcao");
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        try {
                            HTMLDocument documento = (HTMLDocument) edtConversa.getDocument();
                            documento.remove(0, documento.getLength());
                            habilitarEnvio(true);
                        } catch (Exception e) { Util.registrarErro(getClass().getName(), "receberMensagens", "limpar-conversa", e); }
                    });
                    avisoIndisponivel = false;
                    while (!encerrado) {
                        String mensagem = input.readUTF();
                        inserirMensagem(mensagem);
                        if (mensagem.equals(MSG_MANUTENCAO)) { manutencao = true; break; }
                    }
                }
            } catch (Exception e) {
                if (!encerrado && !manutencao) Util.registrarErro(getClass().getName(), "receberMensagens", "recepcao", e);
                if (!encerrado && !avisoIndisponivel) {
                    inserirMensagem("<b>Servidor indisponível. Reconectando...</b><br>");
                    avisoIndisponivel = true;
                }
            } finally { conectado = false; conexaoRecepcao = null; }
            if (!encerrado) {
                javax.swing.SwingUtilities.invokeLater(() -> habilitarEnvio(false));
                try { Thread.sleep(1000); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
            }
        }
    }

    private void habilitarEnvio(boolean habilitado) {
        btnEnviar.setEnabled(habilitado && !envioEmAndamento);
        txtMensagem.setEnabled(habilitado);
        btnEmoji.setEnabled(habilitado && !envioEmAndamento);
        cbModo.setEnabled(habilitado && !envioEmAndamento);
        removerImagem.setEnabled(!envioEmAndamento && imagemPendente != null);
    }

    private void inserirMensagem(String mensagem) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                HTMLDocument documento = (HTMLDocument) edtConversa.getDocument();
                HTMLEditorKit kit = (HTMLEditorKit) edtConversa.getEditorKit();
                kit.insertHTML(documento, documento.getLength(), mensagem, 0, 0, null);
                edtConversa.setCaretPosition(documento.getLength());
            } catch (Exception e) { Util.registrarErro(getClass().getName(), "inserirMensagem", "renderizacao", e); }
        });
    }

    private void configurarPrevia() {
        javax.swing.JPanel painel = new javax.swing.JPanel(new java.awt.BorderLayout(8, 0));
        painel.add(previa, java.awt.BorderLayout.CENTER);
        painel.add(removerImagem, java.awt.BorderLayout.EAST);
        painel.add(estadoEnvio, java.awt.BorderLayout.SOUTH);
        painel.setBounds(30, 475, 710, 140);
        getContentPane().add(painel);
        removerImagem.addActionListener(e -> { removerSelecao(); habilitarEnvio(conectado); });
        setMinimumSize(new java.awt.Dimension(780, 670));
        setSize(800, 680);
    }

    private void removerSelecao() {
        imagemPendente = null;
        previa.setIcon(null);
        previa.setText("Nenhuma imagem selecionada");
        removerImagem.setEnabled(false);
    }

    private void configurarPopupEmoji() {
        chat.catalogo.Catalogo catalogo;
        try { catalogo = chat.catalogo.Catalogo.local(); }
        catch (IllegalStateException e) {
            previa.setText("Catálogo embarcado inválido: reinstale a distribuição. Texto disponível.");
            Util.registrarErro(getClass().getName(), "configurarPopupEmoji", "catalogo-local", e);
            return;
        }
        verificacaoCatalogo = new chat.catalogo.VerificacaoPublicacao(catalogo, Util.urlFigurinhas);
        javax.swing.JTabbedPane abas = new javax.swing.JTabbedPane();
        String[] categorias = {"unicode", "emoji-gif", "figurinha-png", "figurinha-gif"};
        String[] nomes = {"Unicode", "Emojis GIF", "Figurinhas PNG", "Figurinhas GIF"};
        for (int i = 0; i < categorias.length; i++) {
            javax.swing.JPanel painel = new javax.swing.JPanel(new java.awt.GridLayout(0, 4, 5, 5));
            for (var r : catalogo.recursos()) {
                if (!r.selecionavel() || !r.categoria().equals(categorias[i])) continue;
                javax.swing.JButton botao = new javax.swing.JButton(r.unicode() ? r.caminho() : r.nome());
                botao.setToolTipText(r.nome());
                botao.addActionListener(e -> {
                    if (envioEmAndamento) return;
                    if (r.unicode()) {
                        txtMensagem.replaceSelection(r.caminho());
                        txtMensagem.requestFocusInWindow();
                    } else selecionarImagem(r);
                    popupEmoji.setVisible(false);
                });
                painel.add(botao);
            }
            javax.swing.JScrollPane rolagem = new javax.swing.JScrollPane(painel);
            rolagem.setPreferredSize(new java.awt.Dimension(480, 220));
            abas.addTab(nomes[i], rolagem);
        }
        popupEmoji.add(abas);
        timerCatalogo = new javax.swing.Timer(5000, e -> revalidarCatalogo());
        timerCatalogo.start();
        revalidarCatalogo();
    }

    private void revalidarCatalogo() {
        if (verificacaoCatalogo == null || !verificandoCatalogo.compareAndSet(false, true)) return;
        Thread.ofVirtual().start(() -> {
            boolean valida;
            try { verificacaoCatalogo.conferir(); valida = true; }
            catch (java.io.IOException e) { valida = false; }
            boolean resultado = valida;
            javax.swing.SwingUtilities.invokeLater(() -> {
                if (!resultado) estadoEnvio.setText("Imagens bloqueadas: publicação indisponível/incompatível. Nova tentativa automática.");
                else if (estadoEnvio.getText().startsWith("Imagens bloqueadas:")) estadoEnvio.setText("Catálogo verificado; imagens disponíveis.");
                verificandoCatalogo.set(false);
            });
        });
    }

    private void selecionarImagem(chat.catalogo.Catalogo.Recurso recurso) {
        if (verificacaoCatalogo == null || !recurso.selecionavel()) return;
        imagemPendente = recurso;
        previa.setIcon(null);
        previa.setText(recurso.nome() + " — carregando prévia...");
        removerImagem.setEnabled(true);
        btnEnviar.setEnabled(false);
        Thread.ofVirtual().start(() -> {
            try {
                verificacaoCatalogo.conferir();
                var icone = ImagemCatalogo.carregar(recurso);
                javax.swing.SwingUtilities.invokeLater(() -> {
                    if (imagemPendente != recurso) return;
                    previa.setIcon(icone);
                    previa.setText(recurso.nome() + (recurso.figurinha() ? " — envio independente; texto será preservado" : ""));
                    habilitarEnvio(conectado);
                });
            } catch (Exception e) {
                javax.swing.SwingUtilities.invokeLater(() -> {
                    if (imagemPendente != recurso) return;
                    removerSelecao();
                    previa.setText("Imagem/publicação indisponível; tente novamente em 5 segundos.");
                    habilitarEnvio(conectado);
                });
                Util.registrarErro(getClass().getName(), "selecionarImagem", recurso.id(), e);
            }
        });
    }

    private void enviarSticker(chat.catalogo.Catalogo.Recurso recurso) {
        String mensagem = "<img src='/figurinhas/" + Util.avatar + "' width='20' height='20'>";
        mensagem += "<font color='" + Util.cor + "'> " + Util.escaparHtml(Util.nickname) + "</font><br>";
        mensagem += recurso.imagemHtml() + "<br>";
        enviarMensagem(mensagem);
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
        scrollConversa.setBounds(13, 14, 740, 330);

        lblMensagem.setFont(new java.awt.Font("sansserif", 1, 16)); // NOI18N
        lblMensagem.setText("Mensagem");
        getContentPane().add(lblMensagem);
        lblMensagem.setBounds(30, 420, 100, 40);

        txtMensagem.setFont(new java.awt.Font("SansSerif", 0, 14)); // NOI18N
        txtMensagem.addActionListener(this::txtMensagemActionPerformed);
        getContentPane().add(txtMensagem);
        txtMensagem.setBounds(120, 420, 470, 40);

        cbModo.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        cbModo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Fala", "Grita", "Xinga" }));
        getContentPane().add(cbModo);
        cbModo.setBounds(120, 360, 90, 40);

        lblModo.setFont(new java.awt.Font("sansserif", 1, 18)); // NOI18N
        lblModo.setText("Modo");
        getContentPane().add(lblModo);
        lblModo.setBounds(30, 370, 70, 24);

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
        btnEnviar.setBounds(670, 420, 70, 40);

        btnEmoji.setBackground(new java.awt.Color(204, 204, 204));
        btnEmoji.setFont(new java.awt.Font("SansSerif", 0, 20)); // NOI18N
        btnEmoji.setText("🙂");
        btnEmoji.addActionListener(this::btnEmojiActionPerformed);
        getContentPane().add(btnEmoji);
        btnEmoji.setBounds(600, 420, 60, 40);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void txtMensagemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtMensagemActionPerformed
        gerarEnviarMensagem();
    }//GEN-LAST:event_txtMensagemActionPerformed

    private void btnEnviarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEnviarActionPerformed
        this.gerarEnviarMensagem();
    }//GEN-LAST:event_btnEnviarActionPerformed

    private void btnEnviarKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_btnEnviarKeyPressed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnEnviarKeyPressed

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
