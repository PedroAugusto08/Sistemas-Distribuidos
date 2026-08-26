/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.urna;

import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.Socket;
import javax.swing.JOptionPane;

/**
 *
 * @author Pedro
 */
public class Urna {

    public static void main(String[] args) {
        try{
            String voto = JOptionPane.showInputDialog("Escolha um candidato:\n"
                    + "1 - Candidato 1\n"
                    + "2 - Candidato 2\n"
                    + "3 - Candidato 3\n"
            );
            
            Socket s = new Socket("127.0.0.1", 3322);
            
            ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
            out.writeUTF(voto);
            out.flush();
            
            out.close();
            s.close();
            
            DatagramSocket ds = new DatagramSocket(6667);
            
            byte[] b = new byte[256];
            DatagramPacket pckt = new DatagramPacket(b, b.length);
            
            ds.receive(pckt);
            
            String ipGrupo = new String(pckt.getData(), 0, pckt.getLength());
            
            ds.close();
            
            InetAddress addr = InetAddress.getByName(ipGrupo);
            InetSocketAddress group = new InetSocketAddress(addr, 6668);
            
            NetworkInterface netIf = NetworkInterface.getByName("Wi-fi");
            
            MulticastSocket ms = new MulticastSocket(group.getPort());
            ms.joinGroup(group, netIf);
            
            b = new byte[256];
            pckt = new DatagramPacket(b, b.length);
            ms.receive(pckt);
            
            JOptionPane.showMessageDialog(null, new String(pckt.getData(), 0, pckt.getLength()));
            
            ms.leaveGroup(group, netIf);
            ms.close()
                    ;
        }catch(Exception e){
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Erro na Urna: " + e.getMessage());
        }
    }
}
