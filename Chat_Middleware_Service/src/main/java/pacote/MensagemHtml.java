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

    private static final Pattern IMAGEM = Pattern.compile("<img src=['\"](/figurinhas/[^'\"]+)['\"] width=['\"]([0-9]{1,3})['\"] height=['\"]([0-9]{1,3})['\"]>");

    private static boolean imagemCatalogada(String tag) {
        Matcher m = IMAGEM.matcher(tag);
        if (!m.matches()) return false;
        chat.catalogo.Catalogo catalogo;
        try { catalogo = chat.catalogo.Catalogo.local(); }
        catch (IllegalStateException e) { return false; }
        return catalogo.recursos().stream().anyMatch(r -> !r.unicode()
                && m.group(1).equals("/figurinhas/" + r.caminho())
                && Integer.parseInt(m.group(2)) == r.larguraExibicao()
                && Integer.parseInt(m.group(3)) == r.alturaExibicao());
    }

    private MensagemHtml() { }

    public static void validar(String mensagem) {
        if (mensagem == null || mensagem.isBlank()) throw new IllegalArgumentException("Mensagem vazia.");
        if (tamanhoUtfModificado(mensagem) > 65535) {
            throw new IllegalArgumentException("Mensagem excede o limite TCP de 65535 bytes.");
        }
        for (int i = 0; i < mensagem.length(); i++) {
            char c = mensagem.charAt(i);
            if (Character.isHighSurrogate(c)) {
                if (i + 1 >= mensagem.length() || !Character.isLowSurrogate(mensagem.charAt(i + 1))) {
                    throw new IllegalArgumentException("Unicode inválido.");
                }
                i++;
                continue;
            }
            if (Character.isLowSurrogate(c)) throw new IllegalArgumentException("Unicode inválido.");
            if (Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t') {
                throw new IllegalArgumentException("Caractere de controle inválido.");
            }
        }
        validarImagensNovas(mensagem);
        if (!mensagem.equals(paraExibicao(mensagem))) {
            throw new IllegalArgumentException("HTML não permitido na mensagem.");
        }
    }

    private static final Pattern AVATAR = Pattern.compile(
            "<img src=['\"]/figurinhas/imagens/(?:menino|menina|avatar)\\.png['\"] width=['\"](20|30)['\"] height=['\"]\\1['\"]>");
    // Cabeçalhos produzidos pelos dois clientes. Texto de conteúdo nunca faz parte deste prefixo.
    private static final Pattern CABECALHO = Pattern.compile(
            "<font(?: size=['\"][+]?[12]['\"])? color=['\"](?:#[0-9a-fA-F]{6}|navy|tomato|black|DarkRed|red)['\"]>"
            + "(?:[^<]*|<b>[^<]*</b>)</font>(?:<br\\s*/?>)?");

    private static void validarImagensNovas(String mensagem) {
        validarImagensNovas(mensagem, null);
    }

    // Catálogo injetável para regressões de recursos desativados; produção usa somente o embarcado.
    static void validarImagensNovas(String mensagem, chat.catalogo.Catalogo catalogo) {
        Matcher imagens = IMAGEM.matcher(mensagem);
        int avatares = 0, anexos = 0, fimAvatar = 0;
        while (imagens.find()) {
            String caminho = imagens.group(1);
            if (imagens.group().indexOf('\'') >= 0 && imagens.group().indexOf('"') >= 0)
                throw new IllegalArgumentException("Aspas inconsistentes na imagem.");
            if (caminho.startsWith("/figurinhas/imagens/")) {
                if (++avatares > 1 || imagens.start() != 0 || !AVATAR.matcher(imagens.group()).matches())
                    throw new IllegalArgumentException("Avatar inválido: somente um, no início, em 20x20 ou 30x30.");
                fimAvatar = imagens.end();
                continue;
            }
            if (++anexos > 1) throw new IllegalArgumentException("Somente uma imagem de conteúdo por mensagem.");
            if (catalogo == null) {
                try { catalogo = chat.catalogo.Catalogo.local(); }
                catch (IllegalStateException e) { throw new IllegalArgumentException("Imagens bloqueadas: catálogo embarcado inválido.", e); }
            }
            var recurso = catalogo.recursos().stream().filter(r -> !r.unicode()
                    && caminho.equals("/figurinhas/" + r.caminho())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Imagem não autorizada."));
            if (!recurso.selecionavel() || Integer.parseInt(imagens.group(2)) != recurso.larguraExibicao()
                    || Integer.parseInt(imagens.group(3)) != recurso.alturaExibicao())
                throw new IllegalArgumentException("Imagem desativada ou dimensões não autorizadas.");
            if (recurso.figurinha()) {
                String prefixo = mensagem.substring(fimAvatar, imagens.start());
                String sufixo = mensagem.substring(imagens.end());
                boolean cabecalho = fimAvatar > 0 && CABECALHO.matcher(prefixo).matches();
                if (!(prefixo.isEmpty() || prefixo.matches("<br\\s*/?>") || cabecalho)
                        || !sufixo.matches("(?:<br\\s*/?>)?"))
                    throw new IllegalArgumentException("Figurinhas devem ser mensagens independentes.");
            }
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
        Matcher imagem = IMAGEM.matcher(mensagem);
        for (int i = 0; i < mensagem.length();) {
            if (mensagem.charAt(i) == '<') {
                // Reconhece somente a tag nesta posição, sem buscar o próximo >
                // nem copiar sufixos. As alternativas têm prefixos limitados; o
                // único trecho variável (espaços em <br>) não contém outro <.
                if (tag.region(i, mensagem.length()).lookingAt()) {
                    resultado.append(mensagem, i, tag.end());
                    i = tag.end();
                } else if (imagem.region(i, mensagem.length()).lookingAt()
                        && imagemCatalogada(mensagem.substring(i, imagem.end()))) {
                    resultado.append(mensagem, i, imagem.end());
                    i = imagem.end();
                } else {
                    resultado.append("&lt;");
                    i++;
                }
            } else resultado.append(mensagem.charAt(i++));
        }
        return resultado.toString();
    }
}
