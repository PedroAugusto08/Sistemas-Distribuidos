package com.mycompany.sockets02_sender_udp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import javax.swing.JOptionPane;

public class Sockets02_Sender_UDP {

    public static void main(String[] args) {
        try {
            byte[] msg = JOptionPane.showInputDialog("msg a enviar: ").getBytes();

            int port = 6666;

            InetAddress addr = InetAddress.getByName("200.128.142.9");

            DatagramPacket pckt = new DatagramPacket(msg,msg.length,addr,port);

            DatagramSocket ds = new DatagramSocket();

            ds.send(pckt);

            System.out.println("Enviada a: " + addr.getHostAddress()+ " na porta: " + port);

            ds.close();

        } catch (Exception e) {
            e.printStackTrace();

            JOptionPane.showMessageDialog(null, "Erro no emissor: " + e.getMessage());
        }
    }
}