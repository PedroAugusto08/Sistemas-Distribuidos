/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.sockets01_client;

import java.io.ObjectInputStream;
import java.net.Socket;
import javax.swing.JOptionPane;

/**
 *
 * @author Pedro
 */
public class ClientTCP {
    public void Execute(){
        try{
            Socket client = new Socket("127.0.0.1", 3322); // Ip do servidor e a Porta do Servidor.
            ObjectInputStream reader = new ObjectInputStream(client.getInputStream()); // Tipo input permite recepção de dados do servidor.
            JOptionPane.showMessageDialog(null, "Mensagem recebida no cliente: "+ reader.readUTF());
            
            reader.close(); // Fecha a Conexão.
            client.close(); // Fecha a Conexão.
        }
        catch(Exception Error){
            JOptionPane.showMessageDialog(null, "Erro no Cliente: " + Error.getMessage());
        }
    }
}
