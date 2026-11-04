package pacote;

public class Util {
    public static String ipServidor = "localhost";
    public static String nickname = "";
    public static String cor = "";
    public static String avatar = "";
    
    // Configurável na inicialização. O padrão atende ao servidor local.
    public static String urlFigurinhas = System.getProperty(
        "chat.figurinhas.url",
        "http://localhost:8080/figurinhas"
    );
}
