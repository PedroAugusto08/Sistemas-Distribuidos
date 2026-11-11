package pacote;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import chat.swing.TemaSwing;
import java.awt.*;
import javax.swing.*;
import javax.swing.text.html.HTMLDocument;

public class frmChat extends javax.swing.JFrame {
    public String msg = "";
    private long liberarEnvioEm;
    private boolean catalogoDisponivel;
    private final java.util.Map<chat.catalogo.Catalogo.Recurso, JButton> botoesImagem = new java.util.LinkedHashMap<>();
    private final JLabel estadoConexao = TemaSwing.apoio("Conectando ao Desktop…");
    private final JLabel estadoCatalogo = TemaSwing.apoio("Verificando imagens…");
    private final JButton novas = new JButton("Novas mensagens ↓");
    private final javax.swing.JLabel estadoEnvio = new javax.swing.JLabel();
    private JPanel avisos;
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
        if (txtMensagem.getText().isBlank()) return;

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

        this.msg += "<br>";

        enviarMensagem(this.msg, false);
    }

    private void enviarMensagem(String mensagem, boolean possuiImagem) {
        if (envioEmAndamento || System.nanoTime() < liberarEnvioEm || !conectado || encerrado) return;
        try { chat.catalogo.Catalogo.validarMensagem(mensagem); }
        catch (IllegalArgumentException e) { mostrarFeedback(e.getMessage()); return; }
        envioEmAndamento = true;
        mostrarFeedback("");
        habilitarEnvio(conectado);
        Thread.ofVirtual().start(() -> {
            Socket cliente = new Socket();
            Thread prazo = Thread.ofVirtual().start(() -> {
                try { Thread.sleep(5000); cliente.close(); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                catch (java.io.IOException e) { /* Já encerrado. */ }
            });
            boolean escritaIniciada = false;
            try (cliente) {
                if (possuiImagem) {
                    if (verificacaoCatalogo == null) throw new java.io.IOException("Catálogo embarcado inválido.");
                    verificacaoCatalogo.conferir();
                }
                // A partir da conexão, qualquer falha pode deixar o resultado incerto.
                cliente.connect(new java.net.InetSocketAddress(Util.ipServidor, Integer.getInteger("chat.desktop.envio.port", 6662)), 2000);
                try (ObjectOutputStream output = new ObjectOutputStream(cliente.getOutputStream())) {
                    escritaIniciada = true;
                    output.writeUTF(mensagem);
                    output.flush();
                }
                javax.swing.SwingUtilities.invokeLater(() -> mostrarFeedback(feedbackEnvio(false, false)));
            } catch (Exception e) {
                if (!encerrado) Util.registrarErro(getClass().getName(), "enviarMensagem", "envio", e);
                String aviso = feedbackEnvio(true, escritaIniciada);
                if (!encerrado) javax.swing.SwingUtilities.invokeLater(() -> mostrarFeedback(aviso));
            } finally {
                prazo.interrupt();
                javax.swing.SwingUtilities.invokeLater(() -> {
                    // Janela curta após a conclusão evita o segundo evento de duplo clique.
                    liberarEnvioEm = System.nanoTime() + java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(450);
                    envioEmAndamento = false;
                    javax.swing.Timer liberar = new javax.swing.Timer(450, e -> habilitarEnvio(conectado));
                    liberar.setRepeats(false);
                    liberar.start();
                    habilitarEnvio(conectado);
                });
            }
        });
    }

    public frmChat() {
        TemaSwing.instalar();
        initComponents();
        SwingUtilities.updateComponentTreeUI(this);
        setMinimumSize(new Dimension(640, 480));
        setSize(920, 720);
        setLocationRelativeTo(null);
      
        javax.swing.SwingUtilities.invokeLater(() -> txtMensagem.requestFocusInWindow());
        getRootPane().setDefaultButton(btnEnviar);
        ((javax.swing.text.DefaultCaret) edtConversa.getCaret()).setUpdatePolicy(javax.swing.text.DefaultCaret.NEVER_UPDATE);
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
        java.util.List<String> historico = new java.util.ArrayList<>();
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
                    javax.swing.SwingUtilities.invokeLater(() -> habilitarEnvio(true));
                    avisoIndisponivel = false;
                    int indice = 0;
                    java.util.List<String> prefixo = new java.util.ArrayList<>();
                    while (!encerrado) {
                        String mensagem = input.readUTF();
                        if (!mensagem.equals(MSG_MANUTENCAO)) {
                            prefixo.add(mensagem);
                            if (indice < historico.size() && historico.get(indice).equals(mensagem)) {
                                indice++;
                                continue;
                            }
                            if (indice < historico.size()) {
                                historico.clear();
                                historico.addAll(prefixo);
                                javax.swing.SwingUtilities.invokeLater(() -> edtConversa.setText(""));
                                for (String anterior : prefixo) inserirMensagem(anterior);
                            } else {
                                historico.add(mensagem);
                                inserirMensagem(mensagem);
                            }
                            indice++;
                        } else inserirMensagem(mensagem);
                        if (mensagem.equals(MSG_MANUTENCAO)) { manutencao = true; break; }
                    }
                }
            } catch (Exception e) {
                if (!encerrado && !manutencao) Util.registrarErro(getClass().getName(), "receberMensagens", "recepcao", e);
                if (!encerrado && !avisoIndisponivel) {
                    javax.swing.SwingUtilities.invokeLater(() -> estadoConexao.setText("Servidor indisponível. Reconectando…"));
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

    static String feedbackEnvio(boolean falha, boolean escritaIniciada) {
        if (!falha) return "";
        return escritaIniciada ? "Não foi possível confirmar o envio. Confira a conversa antes de tentar novamente."
                : "Não foi possível enviar. Tente novamente.";
    }

    private void mostrarFeedback(String texto) {
        estadoEnvio.setText(texto);
        estadoEnvio.setVisible(!texto.isEmpty());
        atualizarAvisos();
    }

    private void atualizarAvisos() {
        avisos.setVisible(estadoEnvio.isVisible() || novas.isVisible());
        avisos.revalidate();
    }

    private void habilitarEnvio(boolean habilitado) {
        boolean livre = !envioEmAndamento && System.nanoTime() >= liberarEnvioEm;
        btnEnviar.setEnabled(habilitado && livre);
        // O rascunho continua editável, mesmo durante conexão e transmissão.
        btnEmoji.setEnabled(livre);
        cbModo.setEnabled(livre);
        for (var b : botoesImagem.values()) b.setEnabled(habilitado && livre && catalogoDisponivel && b.getIcon() != null);
        estadoConexao.setText(habilitado ? "Desktop conectado • " + Util.nickname : "Desktop desconectado • reconectando…");
    }

    private void inserirMensagem(String mensagem) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                var barra = scrollConversa.getVerticalScrollBar();
                boolean noFinal = barra.getValue() + barra.getVisibleAmount() >= barra.getMaximum() - 32;
                int posicao = barra.getValue();
                HTMLDocument documento = (HTMLDocument) edtConversa.getDocument();
                javax.swing.text.Element raiz = documento.getDefaultRootElement();
                for (int i = 0; i < raiz.getElementCount(); i++) {
                    javax.swing.text.Element corpo = raiz.getElement(i);
                    if ("body".equals(corpo.getName())) {
                        documento.insertBeforeEnd(corpo, HistoricoHtml.formatar(mensagem, Util.nickname));
                        break;
                    }
                }
                SwingUtilities.invokeLater(() -> {
                    if (noFinal) barra.setValue(barra.getMaximum());
                    else { barra.setValue(posicao); novas.setVisible(true); atualizarAvisos(); }
                });
            } catch (Exception e) { Util.registrarErro(getClass().getName(), "inserirMensagem", "renderizacao", e); }
        });
    }

    private void configurarPopupEmoji() {
        chat.catalogo.Catalogo catalogo;
        try { catalogo = chat.catalogo.Catalogo.local(); }
        catch (IllegalStateException e) {
            estadoCatalogo.setText("Catálogo inválido: reinstale a distribuição. Texto disponível.");
            Util.registrarErro(getClass().getName(), "configurarPopupEmoji", "catalogo-local", e);
            return;
        }
        verificacaoCatalogo = new chat.catalogo.VerificacaoPublicacao(catalogo, Util.urlFigurinhas);
        javax.swing.JTabbedPane abas = new javax.swing.JTabbedPane();
        String[] nomes = {"Emojis", "Figurinhas"};
        for (int i = 0; i < nomes.length; i++) {
            javax.swing.JPanel painel = TemaSwing.painel(new java.awt.GridLayout(0, 4, 8, 8), 8);
            for (var r : catalogo.recursos()) {
                if (!r.selecionavel() || r.figurinha() != (i == 1)) continue;
                javax.swing.JButton botao = new javax.swing.JButton(r.unicode() ? r.caminho() : "…");
                botao.setPreferredSize(new Dimension(72, 64));
                botao.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
                botao.setToolTipText(r.nome());
                botao.getAccessibleContext().setAccessibleName(r.nome());
                botao.setFocusable(true);
                botao.addActionListener(e -> {
                    if (envioEmAndamento || System.nanoTime() < liberarEnvioEm) return;
                    if (r.unicode()) {
                        txtMensagem.replaceSelection(r.caminho());
                        txtMensagem.requestFocusInWindow();
                    } else enviarSticker(r);
                    popupEmoji.setVisible(false);
                });
                if (!r.unicode()) { botoesImagem.put(r, botao); botao.setEnabled(false); }
                painel.add(botao);
            }
            javax.swing.JScrollPane rolagem = new javax.swing.JScrollPane(painel);
            rolagem.setPreferredSize(new java.awt.Dimension(352, 256));
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
                catalogoDisponivel = resultado;
                estadoCatalogo.setText(resultado ? "Imagens disponíveis" : "Imagens bloqueadas. Verificação em 5 s; texto disponível.");
                habilitarEnvio(conectado);
            });
            if (resultado) carregarMiniaturas();
            javax.swing.SwingUtilities.invokeLater(() -> verificandoCatalogo.set(false));
        });
    }

    // Chamado pelo verificador em background. Falhas são tentadas novamente na próxima verificação.
    private void carregarMiniaturas() {
        for (var item : botoesImagem.entrySet()) {
            if (encerrado) return;
            JButton botao = item.getValue();
            // Estado de carregamento separado do Swing, só este worker acessa o conjunto.
            if (miniaturasCarregadas.contains(item.getKey())) continue;
            try {
                ImageIcon fonte = ImagemCatalogo.carregar(item.getKey());
                double fator = Math.min(1, 48.0 / Math.max(fonte.getIconWidth(), fonte.getIconHeight()));
                ImageIcon miniatura = ImagemCatalogo.escalar(fonte.getImage(), Math.max(1, (int)Math.round(fonte.getIconWidth()*fator)), Math.max(1, (int)Math.round(fonte.getIconHeight()*fator)));
                miniaturasCarregadas.add(item.getKey());
                SwingUtilities.invokeLater(() -> { botao.setText(""); botao.setIcon(miniatura); habilitarEnvio(conectado); });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> { botao.setText("×"); botao.setToolTipText(item.getKey().nome() + " — indisponível"); botao.setEnabled(false); });
                Util.registrarErro(getClass().getName(), "carregarMiniaturas", item.getKey().id(), e);
            }
        }
    }
    private final java.util.Set<chat.catalogo.Catalogo.Recurso> miniaturasCarregadas = new java.util.HashSet<>();

    private void enviarSticker(chat.catalogo.Catalogo.Recurso recurso) {
        String mensagem = "<img src='/figurinhas/" + Util.avatar + "' width='20' height='20'>";
        mensagem += "<font color='" + Util.cor + "'> " + Util.escaparHtml(Util.nickname) + "</font><br>";
        mensagem += recurso.imagemHtml() + "<br>";
        enviarMensagem(mensagem, true);
    }

    private javax.swing.JButton btnEmoji;
    private javax.swing.JButton btnEnviar;
    private javax.swing.JComboBox<String> cbModo;
    private javax.swing.JEditorPane edtConversa;
    private javax.swing.JScrollPane scrollConversa;
    private javax.swing.JTextField txtMensagem;

    // Histórico como foco; estado no cabeçalho e compositor ancorado ao rodapé.
    // Paleta, superfícies, tipografia e ritmo seguem TemaSwing (G1).
    private JPanel criarConteudo() {
        JPanel sala = TemaSwing.painel(new BorderLayout(0, 16), 16);
        JPanel topo = new JPanel(new BorderLayout(16, 0));
        JPanel identidade = new JPanel(new GridLayout(0, 1, 0, 4));
        identidade.add(TemaSwing.titulo("Conversa"));
        identidade.add(estadoConexao);
        topo.add(identidade, BorderLayout.CENTER);
        topo.add(TemaSwing.alternador(), BorderLayout.EAST);
        sala.add(topo, BorderLayout.NORTH);
        edtConversa = new JEditorPane();
        edtConversa.setEditable(false);
        edtConversa.setEditorKit(new HistoricoHtml());
        edtConversa.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        edtConversa.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        edtConversa.setMargin(new Insets(16, 16, 16, 16));
        edtConversa.getAccessibleContext().setAccessibleName("Histórico da conversa");
        scrollConversa = new JScrollPane(edtConversa);
        scrollConversa.setBorder(BorderFactory.createEmptyBorder());
        scrollConversa.getVerticalScrollBar().setUnitIncrement(24);
        sala.add(scrollConversa, BorderLayout.CENTER);
        JPanel rodape = new JPanel(new BorderLayout(0, 8));
        avisos = new JPanel();
        avisos.setLayout(new BoxLayout(avisos, BoxLayout.Y_AXIS));
        estadoEnvio.setVisible(false);
        avisos.setVisible(false);
        novas.setVisible(false);
        novas.addActionListener(e -> { scrollConversa.getVerticalScrollBar().setValue(scrollConversa.getVerticalScrollBar().getMaximum()); novas.setVisible(false); atualizarAvisos(); });
        avisos.add(novas);
        avisos.add(estadoEnvio);
        rodape.add(avisos, BorderLayout.NORTH);
        JPanel compositor = new JPanel(new BorderLayout(8, 0));
        btnEmoji = new JButton("☺");
        btnEmoji.setPreferredSize(new Dimension(48, 48));
        btnEmoji.setToolTipText("Emojis e figurinhas");
        btnEmoji.getAccessibleContext().setAccessibleName("Emojis e figurinhas");
        btnEmoji.addActionListener(e -> popupEmoji.show(btnEmoji, 0, -popupEmoji.getPreferredSize().height - 8));
        compositor.add(btnEmoji, BorderLayout.WEST);
        txtMensagem = new JTextField();
        txtMensagem.setPreferredSize(new Dimension(240, 48));
        txtMensagem.getAccessibleContext().setAccessibleName("Mensagem");
        txtMensagem.addActionListener(e -> gerarEnviarMensagem());
        compositor.add(txtMensagem, BorderLayout.CENTER);
        btnEnviar = new JButton("Enviar →");
        TemaSwing.acao(btnEnviar);
        btnEnviar.addActionListener(e -> gerarEnviarMensagem());
        compositor.add(btnEnviar, BorderLayout.EAST);
        rodape.add(compositor, BorderLayout.CENTER);
        JPanel opcoes = new JPanel(new BorderLayout(8, 0));
        cbModo = new JComboBox<>(new String[]{"Fala", "Grita", "Xinga"});
        cbModo.getAccessibleContext().setAccessibleName("Modo da mensagem");
        opcoes.add(cbModo, BorderLayout.WEST);
        opcoes.add(estadoCatalogo, BorderLayout.CENTER);
        rodape.add(opcoes, BorderLayout.SOUTH);
        sala.add(rodape, BorderLayout.SOUTH);
        return sala;
    }

    private void initComponents() {//GEN-BEGIN:initComponents
        conteudo = criarConteudo();
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Chat • Conversa");
        getContentPane().add(conteudo, java.awt.BorderLayout.CENTER);
        pack();
    }//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel conteudo;
    // End of variables declaration//GEN-END:variables

    public static void main(String[] args) { EventQueue.invokeLater(() -> new frmChat().setVisible(true)); }
}
