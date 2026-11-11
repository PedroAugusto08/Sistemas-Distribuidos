package pacote;

public class run {
    static menuServidor menuservidor;
    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(() -> menuservidor = new menuServidor());
    }
}
