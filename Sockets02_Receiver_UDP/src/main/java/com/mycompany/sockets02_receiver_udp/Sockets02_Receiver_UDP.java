package com.mycompany.sockets02_receiver_udp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import javax.swing.JOptionPane;

public class Sockets02_Receiver_UDP {

    public static void main(String[] args) {
        try {
            int port = 6666;

            DatagramSocket ds = new DatagramSocket(port);

            System.out.println("Ouvindo a porta: " + port);

            byte[] msg = new byte[256];

            DatagramPacket pckt = new DatagramPacket(msg, msg.length);

            ds.receive(pckt);

            ds.close();

            JOptionPane.showMessageDialog(null,new String(pckt.getData()).trim());

        } catch (Exception e) {
            e.printStackTrace();

            JOptionPane.showMessageDialog(
                    null,
                    "Erro no receptor: " + e.getMessage()
            );
        }
    }
}