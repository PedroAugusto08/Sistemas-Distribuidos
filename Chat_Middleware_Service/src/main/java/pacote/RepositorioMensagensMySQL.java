package pacote;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** Histórico compartilhado em RAM, recuperado e persistido no MySQL. */
public class RepositorioMensagensMySQL implements RepositorioMensagens {
    private final String url;
    private final Properties propriedades = new Properties();
    private final List<String> mensagens = new ArrayList<>();

    public RepositorioMensagensMySQL(String url, String usuario, String senha) throws IOException {
        if (url == null || !url.startsWith("jdbc:mysql://") || usuario == null || usuario.isBlank() || senha == null) {
            throw new IOException("Configure CHAT_DB_URL, CHAT_DB_USER e CHAT_DB_PASSWORD.");
        }
        this.url = url;
        propriedades.setProperty("user", usuario);
        propriedades.setProperty("password", senha);
        propriedades.setProperty("connectTimeout", "3000");
        propriedades.setProperty("socketTimeout", "5000");
        propriedades.setProperty("characterEncoding", "UTF-8");
        propriedades.setProperty("sslMode", "REQUIRED");

        try (Connection conexao = conectar();
                var comando = conexao.prepareStatement("SELECT conteudo FROM mensagens ORDER BY id");
                var resultado = comando.executeQuery()) {
            while (resultado.next()) mensagens.add(resultado.getString("conteudo"));
        } catch (SQLException e) {
            throw new IOException("Não foi possível carregar o histórico do MySQL.", e);
        }
    }

    private Connection conectar() throws SQLException {
        return DriverManager.getConnection(url, propriedades);
    }

    @Override
    public synchronized void adicionarMensagem(String mensagem) throws IOException {
        try (Connection conexao = conectar();
                var comando = conexao.prepareStatement("INSERT INTO mensagens (conteudo) VALUES (?)")) {
            comando.setString(1, mensagem);
            comando.executeUpdate();
            // A conexão JDBC inicia em auto-commit; só divulgar após gravar.
            mensagens.add(mensagem);
        } catch (SQLException e) {
            throw new IOException("Não foi possível gravar a mensagem no MySQL.", e);
        }
    }

    @Override
    public synchronized List<String> consultarMensagens(int indice) {
        return new ArrayList<>(mensagens.subList(indice, mensagens.size()));
    }
}
