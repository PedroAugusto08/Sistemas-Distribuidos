package br.edu.cefetmg.jdbcmysql;

import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class JDBCMySQL extends JFrame {

    JTextField campoNome = new JTextField(20);
    JTextField campoTelefone = new JTextField(20);
    JButton botaoAdicionar = new JButton("Adicionar");
    DefaultTableModel modelo = new DefaultTableModel(new String[]{"ID", "Nome", "Telefone"}, 0);
    JTable tabela = new JTable(modelo);

    public JDBCMySQL() {
        setTitle("Cadastro de clientes");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Painel principal
        JPanel principal = new JPanel(new BorderLayout(0, 10));
        principal.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Formulário
        JPanel painel = new JPanel(new GridLayout(3, 2, 10, 10));
        painel.add(new JLabel("Nome:"));
        painel.add(campoNome);
        painel.add(new JLabel("Telefone:"));
        painel.add(campoTelefone);
        painel.add(new JLabel(""));
        painel.add(botaoAdicionar);

        // Tabela
        tabela.setRowHeight(24);
        tabela.setFillsViewportHeight(true);
        tabela.setDefaultEditor(Object.class, null);
        tabela.getColumnModel().getColumn(0).setMaxWidth(60);

        principal.add(painel, BorderLayout.NORTH);
        principal.add(new JScrollPane(tabela), BorderLayout.CENTER);
        setContentPane(principal);

        botaoAdicionar.addActionListener(e -> adicionarCliente());
        listarClientes();
    }

    // Conecta ao servidor MySQL
    public Connection conectar() throws SQLException {
        return DriverManager.getConnection("jdbc:mysql://10.210.215.143:3306/teste", "root", "root");
    }

    // Adiciona um cliente
    public void adicionarCliente() {
        String nome = campoNome.getText();
        String telefone = campoTelefone.getText();

        if (nome.isBlank() || telefone.isBlank()) {
            JOptionPane.showMessageDialog(this, "Preencha todos os campos.");
            return;
        }

        try (Connection conexao = conectar(); PreparedStatement pstm = conexao.prepareStatement("INSERT INTO clientes(nome, telefone) VALUES (?, ?)")) {
            pstm.setString(1, nome);
            pstm.setString(2, telefone);
            pstm.executeUpdate();

            campoNome.setText("");
            campoTelefone.setText("");
            listarClientes();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        }
    }

    // Lista todos os clientes na tabela
    public void listarClientes() {
        try (Connection conexao = conectar(); PreparedStatement pstm = conexao.prepareStatement("SELECT id, nome, telefone FROM clientes ORDER BY id"); ResultSet rs = pstm.executeQuery()) {
            modelo.setRowCount(0);

            while (rs.next()) {
                modelo.addRow(new Object[]{rs.getInt("id"), rs.getString("nome"), rs.getString("telefone")});
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new JDBCMySQL().setVisible(true));
    }
}
