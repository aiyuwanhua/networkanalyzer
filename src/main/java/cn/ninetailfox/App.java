package cn.ninetailfox;


import cn.ninetailfox.util.CheckPacketUtil;
import com.alibaba.fastjson.JSONObject;
import jpcap.JpcapCaptor;
import jpcap.NetworkInterface;
import jpcap.packet.Packet;
import jpcap.packet.UDPPacket;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

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

        ExecutorService pool = new ThreadPoolExecutor(10, 10,
                0, TimeUnit.MILLISECONDS,
                new LinkedBlockingDeque<>());

        for (NetworkInterface inter : interfaces) {
            pool.execute(() -> {
                try {
                    JpcapCaptor cap = JpcapCaptor.openDevice(inter, 65535, true, 50);
                    cap.setFilter("dst 192.168.1.4", true);

                    Map<String, JSONObject> portMap = new ConcurrentHashMap<>();
                    Map<String, Boolean> portStatus = new ConcurrentHashMap<>();

                    while (true) {
                        Packet packet = cap.getPacket();
                        if (packet instanceof UDPPacket) {
                            UDPPacket udpPacket = (UDPPacket) packet;
                            String port = String.valueOf(udpPacket.dst_port);
                            System.out.println("udp to: " + port);

                            if (portStatus.get(port) != null) {
                                if (portStatus.get(port)) {
                                    System.out.println("is rtp");
                                    // todo 已经验证是rtp流
                                } else {
                                    System.out.println("not rtp");
                                }
                            } else {
                                System.out.println("checking");
                                JSONObject result = CheckPacketUtil.getRtpInfo(udpPacket.data);
                                if (result.getBoolean("match")) {
                                    if (portMap.get(port) == null) {
                                        JSONObject info = new JSONObject();
                                        info.put("payloadType", result.getInteger("payloadType"));
                                        info.put("ssrc", result.getString("ssrc"));
                                        info.put("lastSequenceNumber", result.getLongValue("sequenceNumber"));
                                        info.put("lastTimestamp", result.getLongValue("timestamp"));
                                        info.put("remainCheckTime", 4);
                                        portMap.put(port, info);
                                        continue;
                                    }
                                    JSONObject portInfo = portMap.get(port);
                                    if (portInfo.getIntValue("remainCheckTime") <= 0) {
                                        portMap.remove(port);
                                        portStatus.put(port, true);
                                        continue;
                                    }
                                    long deltaSequenceNumber = result.getLong("sequenceNumber") - portInfo.getLong("lastSequenceNumber");
                                    long deltaTimestamp = result.getLong("timestamp") - portInfo.getLong("lastTimestamp");
                                    boolean sequenceNumberAndTimestampMatch = (deltaSequenceNumber > 0 && deltaTimestamp > 0) || (deltaSequenceNumber < 0 && deltaTimestamp < 0);
                                    if (portInfo.getIntValue("payloadType") != (result.getIntValue("payloadType"))
                                            || !portInfo.getString("ssrc").equals(result.getString("ssrc"))
                                            || !sequenceNumberAndTimestampMatch) {
                                        portMap.remove(port);
                                        portStatus.put(port, false);
                                        continue;
                                    }
                                    portInfo.put("lastSequenceNumber", result.getLong("sequenceNumber"));
                                    portInfo.put("lastTimestamp", result.getLong("timestamp"));
                                    portInfo.put("remainCheckTime", portInfo.getIntValue("remainCheckTime") - 1);
                                } else {
                                    portMap.remove(port);
                                    portStatus.put(port, false);
                                }
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
