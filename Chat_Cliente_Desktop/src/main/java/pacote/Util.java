package pacote;

public class Util {
    public static String ipServidor = "localhost";
    public static String urlColetorErros = System.getProperty("chat.logs.url", "http://localhost:8082/erros/clientes");

    public static void registrarErro(String classe, String metodo, String contexto, Exception erro) {
        RelatorErrosDesktop.padrao().registrar(urlColetorErros, classe, metodo, contexto, erro);
    }

    public static String nickname = "";
    public static String cor = "";
    public static String avatar = "";
    
    // As imagens ficam no docroot do GlassFish e são compartilhadas por URL com o Web.
    // Configurável na inicialização. O padrão atende ao servidor local.
    public static String urlFigurinhas = System.getProperty(
        "chat.figurinhas.url",
        "http://localhost:8080/figurinhas"
    );
    public static String escaparHtml(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
