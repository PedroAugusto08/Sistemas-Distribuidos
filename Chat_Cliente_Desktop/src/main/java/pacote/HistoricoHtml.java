package pacote;

import chat.swing.TemaSwing;
import java.awt.*;
import javax.swing.text.*;
import javax.swing.text.html.*;

/** Mantém o leitor HTML do Swing e suas ImageViews animadas; sem reinterpretar o TXT. */
final class HistoricoHtml extends HTMLEditorKit {
    HistoricoHtml() {
        getStyleSheet().addRule("body { font-family: Segoe UI; font-size: 14pt; margin: 8px; }");
        getStyleSheet().addRule("div.mensagem { padding: 12px; margin-bottom: 12px; }");
        getStyleSheet().addRule("div.mensagem-propria { padding: 12px; margin-bottom: 12px; }");
    }
    // Reconhece apenas o cabeçalho gerado pelo Desktop; HTML legado fica intacto.
    private static final java.util.regex.Pattern CABECALHO = java.util.regex.Pattern.compile(
            "^(<img src='/figurinhas/imagens/(?:menino|menina|avatar)\\.png' width='20' height='20'>)"
            + "(<font color='#[0-9a-fA-F]{6}'> )([^<>]*)(</font>)(?=(?:<b> Fala: </b>|<b><u> Grita: </u></b>|<b><i><u> Xinga: </u></i></b>|<br>))");
    static String formatar(String html, String nickname) {
        var m = CABECALHO.matcher(html);
        if (!m.find() || html.substring(m.end()).contains("/figurinhas/imagens/"))
            return "<div class='mensagem'>" + html + "</div>";
        boolean propria = m.group(3).equals(Util.escaparHtml(nickname));
        String resto = html.substring(m.end());
        // Remove apenas o marcador estrutural gerado pelo Desktop; o texto seguinte é preservado.
        if (resto.startsWith("<b> Fala: </b>")) resto = resto.substring("<b> Fala: </b>".length());
        else if (resto.startsWith("<br>")) resto = resto.substring(4);
        return "<div class='" + (propria ? "mensagem-propria" : "mensagem") + "'>"
                + m.group(1) + m.group(2) + m.group(3) + m.group(4) + "<br>" + resto + "</div>";
    }
    private final ViewFactory fabrica = new HTMLFactory() {
        @Override public View create(Element e) {
            Object tag = e.getAttributes().getAttribute(StyleConstants.NameAttribute);
            if (tag == HTML.Tag.IMG) {
                ImageView imagem = new ImageView(e);
                imagem.setLoadsSynchronously(false);
                return imagem;
            }
            if (tag == HTML.Tag.CONTENT) return new InlineView(e) {
                @Override public Color getForeground() { return TemaSwing.texto(); }
            };
            if (tag == HTML.Tag.BODY) return new BlockView(e, View.Y_AXIS) {
                @Override protected void layoutMinorAxis(int alvo, int eixo, int[] offsets, int[] spans) {
                    super.layoutMinorAxis(alvo, eixo, offsets, spans);
                    for (int i = 0; i < getViewCount(); i++) {
                        View filho = getView(i);
                        Object classe = filho.getElement().getAttributes().getAttribute(HTML.Attribute.CLASS);
                        if (!"mensagem".equals(classe) && !"mensagem-propria".equals(classe)) continue;
                        int largura = Math.min((int)(alvo * .78), (int)Math.ceil(filho.getPreferredSpan(View.X_AXIS)));
                        spans[i] = Math.max(1, Math.min(alvo, largura));
                        offsets[i] = "mensagem-propria".equals(classe) ? alvo - spans[i] : 0;
                    }
                }
            };
            Object classe = e.getAttributes().getAttribute(HTML.Attribute.CLASS);
            if (tag == HTML.Tag.DIV && ("mensagem".equals(classe) || "mensagem-propria".equals(classe))) {
                return new BlockView(e, View.Y_AXIS) {
                    @Override public float getMaximumSpan(int eixo) {
                        return eixo == View.X_AXIS ? Math.min(660, super.getMaximumSpan(eixo)) : super.getMaximumSpan(eixo);
                    }
                    @Override public float getAlignment(int eixo) { return eixo == View.X_AXIS ? 0 : super.getAlignment(eixo); }
                    @Override public void paint(Graphics g, Shape area) {
                        Rectangle r = area.getBounds();
                        Graphics2D copia = (Graphics2D) g.create();
                        try {
                            copia.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            copia.setColor("mensagem-propria".equals(classe) ? TemaSwing.mensagemPropria() : TemaSwing.elevado());
                            copia.fillRoundRect(r.x, r.y, r.width, r.height - 8, 16, 16);
                        } finally { copia.dispose(); }
                        super.paint(g, area);
                    }
                };
            }
            return super.create(e);
        }
    };
    @Override public ViewFactory getViewFactory() { return fabrica; }
}
