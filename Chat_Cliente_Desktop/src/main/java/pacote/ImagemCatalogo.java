package pacote;

import chat.catalogo.Catalogo.Recurso;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;

/** Carregamento fora da EDT; cache limitado aos 13 recursos visuais selecionáveis. */
final class ImagemCatalogo {
    private static final Map<String, ImageIcon> CACHE = new LinkedHashMap<>(13, .75f, true) {
        protected boolean removeEldestEntry(Map.Entry<String, ImageIcon> e) { return size() > 13; }
    };
    static synchronized ImageIcon carregar(Recurso r) throws IOException {
        String chave = Util.urlFigurinhas + "/" + r.caminho();
        ImageIcon fonte = CACHE.get(chave);
        if (fonte == null) {
            HttpURLConnection con = (HttpURLConnection) URI.create(chave).toURL().openConnection();
            con.setInstanceFollowRedirects(false); con.setConnectTimeout(2000); con.setReadTimeout(3000);
            try {
                if (con.getResponseCode() != 200) throw new IOException("Imagem indisponível.");
                try (InputStream in = con.getInputStream()) {
                    byte[] dados = in.readNBytes(6*1024*1024+1);
                    if (dados.length > 6*1024*1024) throw new IOException("Imagem excessiva.");
                    fonte = new ImageIcon(dados);
                }
                if (fonte.getIconWidth() != r.largura() || fonte.getIconHeight() != r.altura())
                    throw new IOException("Dimensões incompatíveis com catálogo.");
                CACHE.put(chave, fonte);
            } finally { con.disconnect(); }
        }
        return escalar(fonte.getImage(), r.larguraExibicao(), r.alturaExibicao());
    }
    // Desenha a fonte animada, sem ImageIO.read/BufferedImage ou rasterização intermediária.
    // https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/javax/swing/ImageIcon.html
    static ImageIcon escalar(Image imagem, int largura, int altura) {
        return new ImageIcon(imagem) {
            public int getIconWidth() { return largura; }
            public int getIconHeight() { return altura; }
            public synchronized void paintIcon(Component c, Graphics g, int x, int y) {
                g.drawImage(getImage(), x, y, largura, altura, c);
            }
        };
    }
}
