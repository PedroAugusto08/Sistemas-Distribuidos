package pacote;

import java.io.IOException;
import java.util.List;

public interface RepositorioMensagens {
    void adicionarMensagem(String mensagem) throws IOException;
    List<String> consultarMensagens(int indice);
}
