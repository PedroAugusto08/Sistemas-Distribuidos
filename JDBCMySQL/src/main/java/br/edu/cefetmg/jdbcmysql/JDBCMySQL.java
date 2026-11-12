
package br.edu.cefetmg.jdbcmysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import javax.swing.JOptionPane;

public class JDBCMySQL {
    public static void main(String[] args) throws SQLException {
        // Entrada de dados
        String nome = JOptionPane.showInputDialog("Digite o nome:");
        String telefone = JOptionPane.showInputDialog("Digite o telefone:");

        // Conecta ao servidor de banco de dados
        Connection conexao = DriverManager.getConnection("jdbc:mysql://10.112.75.143:3307/teste", "root", "root");

        // Executa comando SQL
        if (nome != null && telefone != null && !nome.isBlank() && !telefone.isBlank()) {
            PreparedStatement stm = conexao.prepareStatement("INSERT INTO clientes(nome, telefone) VALUES (?, ?)");
            stm.setString(1, nome);
            stm.setString(2, telefone);
            stm.executeUpdate();
            stm.close();
        }

        // Mostra todos os clientes antes da exclusão
        PreparedStatement pstm = conexao.prepareStatement("SELECT nome, telefone FROM clientes");
        ResultSet rs = pstm.executeQuery();
        System.out.println("=== CLIENTES ===");
        while (rs.next()) {
            System.out.println("Nome: " + rs.getString("nome"));
            System.out.println("Telefone: " + rs.getString("telefone"));
        }
        rs.close();
        pstm.close();

        // Exclui cliente pelo nome
        String nomeExcluir = JOptionPane.showInputDialog("Digite o nome que deseja excluir:");
        if (nomeExcluir != null && !nomeExcluir.isBlank()) {
            PreparedStatement excluir = conexao.prepareStatement("DELETE FROM clientes WHERE nome = ?");
            excluir.setString(1, nomeExcluir);
            System.out.println("\nExcluidos: " + excluir.executeUpdate());
            excluir.close();
        }

        // Mostra todos os clientes depois da exclusão
        pstm = conexao.prepareStatement("SELECT nome, telefone FROM clientes");
        rs = pstm.executeQuery();
        System.out.println("\n=== CLIENTES DEPOIS DA EXCLUSÃO ===");
        while (rs.next()) {
            System.out.println("Nome: " + rs.getString("nome"));
            System.out.println("Telefone: " + rs.getString("telefone"));
        }

        rs.close();
        pstm.close();
        conexao.close();
    }
}
