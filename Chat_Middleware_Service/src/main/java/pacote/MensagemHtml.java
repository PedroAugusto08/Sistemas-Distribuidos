package pacote;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Vocabulário visual do chat, sem scripts, URLs externas ou atributos livres. */
public final class MensagemHtml {
    private static final Pattern TAG = Pattern.compile(
            "</?(?:b|i|u)>|<br\\s*/?>|</font>"
            + "|<font(?: size=['\"][+]?[12]['\"])? color=['\"](?:#[0-9a-fA-F]{6}|navy|tomato|black|DarkRed|red)['\"](?: size=['\"][+]?[12]['\"])?>"
            + "|<img src=['\"]/figurinhas/(?:imagens/(?:menino|menina|avatar)\\.png"
            + "|emojis/(?:coracao|risada|choro)\\.gif"
            + "|stickers/(?:baby|cat|JJJ|sigma|thinking|shrek|cristiano)\\.gif)['\"]"
            + " width=['\"](?:20|30|128)['\"] height=['\"](?:20|30|128)['\"]>");

    private MensagemHtml() { }

    public static void validar(String mensagem) {
        if (mensagem == null || mensagem.isBlank()) throw new IllegalArgumentException("Mensagem vazia.");
        if (tamanhoUtfModificado(mensagem) > 65535) {
            throw new IllegalArgumentException("Mensagem excede o limite TCP de 65535 bytes.");
        }
        for (int i = 0; i < mensagem.length(); i++) {
            char c = mensagem.charAt(i);
            if (Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t') {
                throw new IllegalArgumentException("Caractere de controle inválido.");
            }
        }
        if (!mensagem.equals(paraExibicao(mensagem))) {
            throw new IllegalArgumentException("HTML não permitido na mensagem.");
        }
    }

    /** Número de bytes do writeUTF, sem os dois bytes do prefixo de tamanho. */
    public static long tamanhoUtfModificado(String mensagem) {
        long bytes = 0;
        for (int i = 0; i < mensagem.length(); i++) {
            char c = mensagem.charAt(i);
            // NUL ocupa dois bytes; um par substituto (emoji) ocupa seis.
            bytes += c >= 1 && c <= 127 ? 1 : c <= 2047 ? 2 : 3;
        }
        return bytes;
    }

    /** Filtra também históricos antigos, sem reescrever o TXT nem a RAM. */
    public static String paraExibicao(String mensagem) {
        StringBuilder resultado = new StringBuilder(mensagem.length());
        Matcher tag = TAG.matcher(mensagem);
        for (int i = 0; i < mensagem.length();) {
            if (mensagem.charAt(i) == '<') {
                // Reconhece somente a tag nesta posição, sem buscar o próximo >
                // nem copiar sufixos. As alternativas têm prefixos limitados; o
                // único trecho variável (espaços em <br>) não contém outro <.
                if (tag.region(i, mensagem.length()).lookingAt()) {
                    resultado.append(mensagem, i, tag.end());
                    i = tag.end();
                } else {
                    resultado.append("&lt;");
                    i++;
                }
            } else resultado.append(mensagem.charAt(i++));
        }
        return resultado.toString();
    }
}
