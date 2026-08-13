/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.sockets01_server;

import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import javax.swing.JOptionPane;

/**
 *
 * @author Pedro
 */
public class ServidorTCP {
    public void Execute(){
        try{
            ServerSocket server = new ServerSocket(3322); // Cria um socket para a porta 3322, porém ainda fechada
            JOptionPane.showMessageDialog(null, "Servidor Iniciado!");
            Socket client = server.accept();  // Abre a porta 3322 para aceitar conexões;
            ObjectOutputStream writer = new ObjectOutputStream(client.getOutputStream()); // tipo outpur permite saída de dados do servidor
            writer.flush(); //Opcional -> Limpa lixo da conexão.
            writer.writeUTF("Você conectou-se com sucesso ao servidor - Bem Vindo!");
            
            writer.close(); // Fecha a Conexão.
            client.close(); // Fecha a Conexão.
        }
        
        catch(Exception Error){
            // O null indica pra qual janela deve se sobrescrever a mensagem.
            JOptionPane.showMessageDialog(null, "Erro no Servidor: "+ Error.getMessage());
        }
    }
}
