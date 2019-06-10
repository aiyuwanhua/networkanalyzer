package cn.ninetailfox;


import cn.ninetailfox.util.TranscodingUtil;
import jpcap.JpcapCaptor;
import jpcap.NetworkInterface;
import jpcap.packet.IPPacket;
import jpcap.packet.Packet;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Hello world!
 *
 */
public class App 
{
    private static final String FILTER = "";

    public static void main( String[] args ) {
        NetworkInterface[] interfaces = JpcapCaptor.getDeviceList();
        for (NetworkInterface networkInterface : interfaces) {
            System.out.printf("%-30s %s\n", networkInterface.name, networkInterface.description);
        }

        ExecutorService pool = new ThreadPoolExecutor(5, 5,
                0, TimeUnit.MILLISECONDS,
                new LinkedBlockingDeque<>());

        for (NetworkInterface inter : interfaces) {
            pool.execute(() -> {
                try {
                    JpcapCaptor cap = JpcapCaptor.openDevice(inter, 65535, true, 50);
                    cap.setFilter("dst 192.168.1.4", true);
                    while (true) {
                        Packet packet = cap.getPacket();
/*                        if (packet instanceof UDPPacket) {
                            UDPPacket udpPacket = (UDPPacket) packet;
                            System.out.println(udpPacket);
                            System.out.println(TranscodingUtil.byteArray2HexString(udpPacket.header));
                            System.out.println(TranscodingUtil.byteArray2HexString(udpPacket.data));
                            System.out.println();
                        }*/
                        if (packet instanceof IPPacket) {
                            IPPacket ipPacket = (IPPacket) packet;
                            if (ipPacket.protocol == 17) {
                                System.out.println(ipPacket);
                                System.out.println(TranscodingUtil.byteArray2HexString(ipPacket.header));
                                System.out.println(TranscodingUtil.byteArray2HexString(ipPacket.data));
                                System.out.println();
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

    }
}
