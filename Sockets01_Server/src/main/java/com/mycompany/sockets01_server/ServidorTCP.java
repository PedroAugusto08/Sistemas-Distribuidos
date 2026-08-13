/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.sockets01_server;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import javax.swing.JOptionPane;
import java.util.ArrayList;

/**
 *
 * @author Pedro
 */
public class ServidorTCP {
    public void Execute(){
        try{
            ServerSocket server = new ServerSocket(3322); // Cria um socket para a porta 3322, porém ainda fechada
            
            JOptionPane.showMessageDialog(null, "Servidor Iniciado!");
            
            ArrayList<String> mensagens = new ArrayList<>();
            
            for(int i = 1; i <= 2; i++) {
                JOptionPane.showMessageDialog(null, "Aguardando cliente" + i + "...");
            
                Socket client = server.accept();  // Abre a porta 3322 para aceitar conexões;
                
                ObjectOutputStream writer = new ObjectOutputStream(client.getOutputStream()); // tipo outpur permite saída de dados do servidor
                
                ObjectInputStream reader = new ObjectInputStream(client.getInputStream());
            
                writer.flush(); //Opcional -> Limpa lixo da conexão
            
                String msg = JOptionPane.showInputDialog("Mensagem para o cliente " + i);
                mensagens.add("Servidor -> Cliente " + i + ": " + msg);
            
                msg += "\n\n Seu IP é: " + client.getInetAddress().getHostAddress();
            
                writer.writeUTF(msg);
                writer.flush();
                
                // Espera resposta do cliente
                String resposta = reader.readUTF();
                mensagens.add("Cliente " + i + " (" + client.getInetAddress().getHostAddress() + ")" + ": " + resposta);
                
                JOptionPane.showMessageDialog(null,"Resposta do cliente " + i + ": " + resposta);
            
                reader.close();
                writer.close(); // Fecha a Conexão.
                client.close(); // Fecha a Conexão.
           }
            
            String historico = "";
            
            for(String mensagem : mensagens) {
                historico += mensagem + "\n";
            }
            
            JOptionPane.showMessageDialog(null,"Histórico:\n\n" + historico);
            
            server.close();
            
            JOptionPane.showMessageDialog(null, "As duas mensagens foram enviadas");
            
        }catch(Exception Error){
            // O null indica pra qual janela deve se sobrescrever a mensagem.
            JOptionPane.showMessageDialog(null, "Erro no Servidor: "+ Error.getMessage());
        }
    }
}
