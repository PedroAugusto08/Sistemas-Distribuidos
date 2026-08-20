package com.mycompany.sockets03_sender_udp_multicast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import javax.swing.JOptionPane;

public class Sockets03_Sender_UDP_multicast {
    public static void main(String[] args) {
        try {
            byte[] b = JOptionPane.showInputDialog("msg: ").getBytes();
            InetAddress addr = InetAddress.getByName("239.0.0.10");
            
            DatagramSocket ds = new DatagramSocket();
            DatagramPacket pckt = new DatagramPacket(b, b.length, addr, 6668);
            
            ds.send(pckt);
            System.out.println("Mensagem enviada");
            ds.close();
            
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Erro no emissor: " + e.getMessage());
        }
    }
}