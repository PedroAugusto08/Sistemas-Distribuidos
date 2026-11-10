package chat.catalogo;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/** Contrato M6, sem dependências. O catálogo embarcado é a autorização local confiável. */
public final class Catalogo {
    public record Recurso(String id, String categoria, String nome, String caminho,
                          String formato, int largura, int altura, boolean selecionavel) {
        public boolean unicode() { return categoria.equals("unicode"); }
        public boolean figurinha() { return categoria.startsWith("figurinha-"); }
        public int larguraExibicao() { return escala()[0]; }
        public int alturaExibicao() { return escala()[1]; }
        private int[] escala() {
            int max = figurinha() ? 128 : 30;
            double fator = Math.min(1, (double) max / Math.max(largura, altura));
            return new int[]{Math.max(1, (int)Math.round(largura*fator)), Math.max(1, (int)Math.round(altura*fator))};
        }
        public String imagemHtml() {
            if (unicode()) throw new IllegalArgumentException("Recurso Unicode não é imagem.");
            return "<img src='/figurinhas/" + caminho + "' width='" + larguraExibicao()
                    + "' height='" + alturaExibicao() + "'>";
        }
    }
    private final List<Recurso> recursos;
    private final String assinatura;
    private Catalogo(List<Recurso> recursos, byte[] bytes) {
        this.recursos = List.copyOf(recursos);
        try { assinatura = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private static final class Local {
        static Catalogo valor;
        static RuntimeException erro;
        static {
            try { valor = carregarLocal(); }
            catch (RuntimeException e) {
                erro = e;
                System.getLogger(Catalogo.class.getName()).log(System.Logger.Level.ERROR,
                        "Catálogo embarcado ausente ou inválido; imagens bloqueadas. Reinstale a distribuição íntegra.", e);
            }
        }
    }
    public static Catalogo local() {
        if (Local.erro != null) throw new IllegalStateException(
                "Catálogo embarcado ausente ou inválido; reinstale a distribuição íntegra.", Local.erro);
        return Local.valor;
    }
    private static Catalogo carregarLocal() {
        try (InputStream in = Catalogo.class.getResourceAsStream("/catalogo.tsv")) {
            if (in == null) throw new IOException("Catálogo local ausente.");
            return ler(in);
        } catch (IOException e) { throw new IllegalStateException("Catálogo local inválido.", e); }
    }
    public List<Recurso> recursos() { return recursos; }
    public String assinatura() { return assinatura; }
    public Recurso selecionar(String id) {
        return recursos.stream().filter(r -> r.id().equals(id) && r.selecionavel()).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Recurso indisponível."));
    }
    /** TSV UTF-8 estrito: cabeçalho e nove colunas; sem escapes nem caminhos livres. */
    public static Catalogo ler(InputStream in) throws IOException {
        byte[] bytes = in.readNBytes(65537);
        if (bytes.length > 65536) throw new IOException("Catálogo excessivo.");
        String texto;
        try { texto = StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString(); }
        catch (java.nio.charset.CharacterCodingException e) { throw new IOException("UTF-8 inválido.", e); }
        String[] linhas = texto.split("\\R");
        if (linhas.length < 2 || !linhas[0].equals("chat-catalogo\t1")) throw new IOException("Versão de catálogo incompatível.");
        List<Recurso> itens = new ArrayList<>();
        Set<String> ids = new HashSet<>(), caminhos = new HashSet<>(), unicode = new HashSet<>();
        for (int i = 1; i < linhas.length; i++) {
            String[] c = linhas[i].split("\t", -1);
            try {
                if (c.length != 9 || itens.size() >= 256 || !c[0].matches("[a-z0-9][a-z0-9-]{0,47}")
                        || !ids.add(c[0]) || !c[2].matches("[^<>\\p{Cntrl}]{1,60}")
                        || !Set.of("true", "false").contains(c[8])) throw new IllegalArgumentException();
                int w = Integer.parseInt(c[5]), h = Integer.parseInt(c[6]);
                boolean u = c[1].equals("unicode");
                if (u) {
                    if (!c[3].isEmpty() || !c[4].equals("unicode") || w != 0 || h != 0
                            || c[7].isEmpty() || c[7].length() > 16 || !unicode.add(c[7])
                            || c[7].codePoints().anyMatch(cp -> cp < 0x2000 || cp >= 0xd800 && cp <= 0xdfff))
                        throw new IllegalArgumentException();
                } else {
                    if (!Set.of("emoji-gif", "figurinha-png", "figurinha-gif").contains(c[1])
                            || !Set.of("png", "gif").contains(c[4]) || !c[1].endsWith(c[4])
                            || !c[3].matches("(?:emojis|stickers)/[A-Za-z0-9_-]{1,64}\\.(?:png|gif)")
                            || !c[3].endsWith("."+c[4]) || !c[7].isEmpty() || !caminhos.add(c[3])
                            || w < 1 || h < 1 || w > 2048 || h > 2048
                            || c[1].equals("emoji-gif") != c[3].startsWith("emojis/")) throw new IllegalArgumentException();
                }
                itens.add(new Recurso(c[0], c[1], c[2], u ? c[7] : c[3], c[4], w, h, Boolean.parseBoolean(c[8])));
            } catch (IllegalArgumentException e) { throw new IOException("Entrada inválida na linha " + (i+1), e); }
        }
        return new Catalogo(itens, bytes);
    }
    /** Sem redirects: clientes só habilitam imagens quando a publicação corresponde ao artefato. */
    public void conferirPublicacao(String base) throws IOException {
        URI uri;
        try { uri = URI.create(base + "/catalogo.tsv"); }
        catch (IllegalArgumentException e) { throw new IOException("URL inválida.", e); }
        if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) || uri.getHost() == null
                || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null)
            throw new IOException("URL inválida.");
        HttpURLConnection con = (HttpURLConnection) uri.toURL().openConnection();
        con.setInstanceFollowRedirects(false); con.setConnectTimeout(2000); con.setReadTimeout(3000);
        con.setUseCaches(false); con.setRequestProperty("Cache-Control", "no-cache");
        try {
            if (con.getResponseCode() != 200) throw new IOException("Catálogo publicado indisponível.");
            try (InputStream in = con.getInputStream()) {
                if (!assinatura.equals(ler(in).assinatura)) throw new IOException("Catálogo publicado incompatível; publique a mesma distribuição.");
            }
        } finally { con.disconnect(); }
    }
    public static void validarMensagem(String mensagem) {
        long bytes = 0;
        for (int i = 0; i < mensagem.length(); i++) {
            char c = mensagem.charAt(i);
            bytes += c >= 1 && c <= 127 ? 1 : c <= 2047 ? 2 : 3;
            if (Character.isHighSurrogate(c)) {
                if (i+1 >= mensagem.length() || !Character.isLowSurrogate(mensagem.charAt(i+1))) throw new IllegalArgumentException("Unicode inválido.");
            } else if (Character.isLowSurrogate(c) && (i == 0 || !Character.isHighSurrogate(mensagem.charAt(i-1)))) throw new IllegalArgumentException("Unicode inválido.");
            if (Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t') throw new IllegalArgumentException("Controle inválido.");
        }
        if (bytes > 65535 || mensagem.getBytes(StandardCharsets.UTF_8).length > 65535) throw new IllegalArgumentException("Mensagem excede 65535 bytes.");
    }
    /** Validação offline antes de publicar. Não inicia serviços nem acessa históricos. */
    public static void main(String[] args) throws IOException {
        if (args.length != 1) throw new IllegalArgumentException("Informe a pasta figurinhas da distribuição.");
        java.nio.file.Path raiz = java.nio.file.Path.of(args[0]).toRealPath();
        Catalogo pacote;
        try (InputStream in = java.nio.file.Files.newInputStream(raiz.resolve("catalogo.tsv"))) { pacote = ler(in); }
        if (!local().assinatura.equals(pacote.assinatura)) throw new IOException("JAR e recursos pertencem a distribuições diferentes.");
        List<Recurso> arquivos = new ArrayList<>(pacote.recursos);
        for (String nome : List.of("avatar", "menino", "menina")) {
            java.nio.file.Path avatar = raiz.resolve("imagens/" + nome + ".png").toRealPath();
            if (!avatar.startsWith(raiz)) throw new IOException("Avatar fora da distribuição.");
            try (var in = javax.imageio.ImageIO.createImageInputStream(avatar.toFile())) {
                var leitores = javax.imageio.ImageIO.getImageReaders(in);
                if (!leitores.hasNext()) throw new IOException("Avatar inválido: " + nome);
                var leitor = leitores.next();
                try {
                    leitor.setInput(in);
                    arquivos.add(new Recurso("avatar-" + nome, "avatar", nome, "imagens/" + nome + ".png",
                            "png", leitor.getWidth(0), leitor.getHeight(0), false));
                } finally { leitor.dispose(); }
            }
        }
        for (Recurso r : arquivos) {
            if (r.unicode()) continue;
            java.nio.file.Path arquivo = raiz.resolve(r.caminho()).toRealPath();
            if (!arquivo.startsWith(raiz) || java.nio.file.Files.size(arquivo) > 6*1024*1024
                    || r.largura() > 2048 || r.altura() > 2048)
                throw new IOException("Arquivo inválido: " + r.id());
            try (var in = javax.imageio.ImageIO.createImageInputStream(arquivo.toFile())) {
                var leitores = javax.imageio.ImageIO.getImageReaders(in);
                if (!leitores.hasNext()) throw new IOException("Formato inválido: " + r.id());
                var leitor = leitores.next();
                try {
                    leitor.setInput(in);
                    int quadros = leitor.getNumImages(true);
                    if (!leitor.getFormatName().equalsIgnoreCase(r.formato()) || leitor.getWidth(0) != r.largura()
                            || leitor.getHeight(0) != r.altura() || quadros > 512
                            || (long)r.largura()*r.altura()*quadros*4 > 128L*1024*1024)
                        throw new IOException("Metadados ou limites inválidos: " + r.id());
                } finally { leitor.dispose(); }
            }
        }
        System.out.println("Catálogo verificado: " + pacote.recursos.size() + " recursos, SHA-256 " + pacote.assinatura);
    }

}
