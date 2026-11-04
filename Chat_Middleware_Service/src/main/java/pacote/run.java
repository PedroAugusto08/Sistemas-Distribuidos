package pacote;

public class run {
    static menuServidor menuservidor;
    
    public static void main(String[] args){
        try {
            Util.inicializarRepositorio();
        } catch (java.io.IOException e) {
            e.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(null, "Erro ao carregar histórico: " + e.getMessage());
            return;
        }
        menuservidor = new menuServidor();
    }
}
