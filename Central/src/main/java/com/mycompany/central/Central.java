/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.central;

import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Locale;
import javax.swing.JOptionPane;

/**
 *
 * @author Pedro
 */
public class Central {

    public static void main(String[] args) {
        try {
            ServerSocket ss = new ServerSocket(3322);

            int candidato1 = 0;
            int candidato2 = 0;
            int candidato3 = 0;
            int candidato4 = 0;
            int branco = 0;
            int nulo = 0;

            int totalVotos = 3;

            System.out.println("Central aguardando votos...");

            for (int i = 0; i < totalVotos; i++) {
                Socket s = ss.accept();

                ObjectInputStream in = new ObjectInputStream(s.getInputStream());

                String voto = in.readUTF();

                System.out.println("Voto recebido: " + voto);

                if (voto.equals("1")) {
                    candidato1++;
                }

                if (voto.equals("2")) {
                    candidato2++;
                }

                if (voto.equals("3")) {
                    candidato3++;
                }

                if (voto.equals("4")) {
                    candidato4++;
                }

                if (voto.equals("5")) {
                    branco++;
                }

                if (voto.equals("6")) {
                    nulo++;
                }

                InetAddress ipUrna = s.getInetAddress();

                String ipGrupo = "239.0.0.15";

                DatagramSocket ds = new DatagramSocket();

                byte[] b = ipGrupo.getBytes();

                DatagramPacket pckt = new DatagramPacket(
                        b,
                        b.length,
                        ipUrna,
                        6667
                );

                ds.send(pckt);

                ds.close();
                in.close();
                s.close();
            }

            ss.close();

            double porcentagem1 = (candidato1 / (double) totalVotos) * 100;
            double porcentagem2 = (candidato2 / (double) totalVotos) * 100;
            double porcentagem3 = (candidato3 / (double) totalVotos) * 100;
            double porcentagem4 = (candidato4 / (double) totalVotos) * 100;
            double porcentagemBranco = (branco / (double) totalVotos) * 100;
            double porcentagemNulo = (nulo / (double) totalVotos) * 100;

            String ganhador;

            if (candidato1 > candidato2 && candidato1 > candidato3 && candidato1 > candidato4) {
                ganhador = "Candidato 1";

            } else if (candidato2 > candidato1 && candidato2 > candidato3 && candidato2 > candidato4) {
                ganhador = "Candidato 2";

            } else if (candidato3 > candidato1 && candidato3 > candidato2 && candidato3 > candidato4) {
                ganhador = "Candidato 3";

            } else if (candidato4 > candidato1 && candidato4 > candidato2 && candidato4 > candidato3) {
                ganhador = "Candidato 4";

            } else {
                ganhador = "Empate";
            }

            String resultado = "RESULTADO DA ELEIÇÃO\n\n"
                    + "Candidato 1: " + candidato1 + " votos (" + String.format(Locale.US, "%.1f", porcentagem1) + "%)\n"
                    + "Candidato 2: " + candidato2 + " votos (" + String.format(Locale.US, "%.1f", porcentagem2) + "%)\n"
                    + "Candidato 3: " + candidato3 + " votos (" + String.format(Locale.US, "%.1f", porcentagem3) + "%)\n"
                    + "Candidato 4: " + candidato4 + " votos (" + String.format(Locale.US, "%.1f", porcentagem4) + "%)\n"
                    + "Brancos: " + branco + " votos (" + String.format(Locale.US, "%.1f", porcentagemBranco) + "%)\n"
                    + "Nulos: " + nulo + " votos (" + String.format(Locale.US, "%.1f", porcentagemNulo) + "%)\n\n"
                    + "Ganhador: " + ganhador;

            System.out.println(resultado);

            InetAddress addr = InetAddress.getByName("239.0.0.10");

            DatagramSocket ds = new DatagramSocket();

            byte[] b = resultado.getBytes();

            DatagramPacket pckt = new DatagramPacket(
                    b,
                    b.length,
                    addr,
                    6668
            );

            ds.send(pckt);

            ds.close();

        } catch (Exception e) {
            e.printStackTrace();

            JOptionPane.showMessageDialog(
                    null,
                    "Erro na Central: " + e.getMessage()
            );
        }
    }
}