package cn.ninetailfox.util;

import com.alibaba.fastjson.JSONObject;

import java.util.Arrays;

public class CheckPacketUtil {

    private static final int FIXED_RTP_HEADER_LENGTH = 12;
    private static final int RTP_VERSION = 0b10;

    private CheckPacketUtil() {}

    public static JSONObject getRtpInfo(byte[] data) {
        /*
         * 1. UDP载荷长度是否大于12字节（RTP报文固定头的长度）
         * 2. 载荷前两位是否是0x10（现行的RTP版本号）
         * 3. 如果CC对应的比特不为0， 判断载荷长度是否大于CC的值的4倍与12的和（载荷长度是否大于固定头和拓展头长度之和）
         * 4. PT对应的比特是否相同
         * 5. SSRC对应的比特是否相同
         * 6. 前后两个载荷中sequence number与timestamp对应的比特大小关系是否一致
         */
        JSONObject result = new JSONObject();
        result.put("match", false);

        int packetLength = data.length;
        if (packetLength < FIXED_RTP_HEADER_LENGTH) {
            return result;
        }
        byte[] fixedHeader = Arrays.copyOfRange(data, 0, FIXED_RTP_HEADER_LENGTH);
        int version = ((fixedHeader[0] & 0xff) >> 6) & 0b11;
//        int padding = ((fixedHeader[0] & 0xff) >> 5) & 0b1;
//        int extend = ((fixedHeader[0] & 0xff) >> 4) & 0b1;
        int csrcCount = fixedHeader[0] & 0xff & 0b1111;
        int payloadType = fixedHeader[1] & 0xff & 0x7f;
        long sequenceNumber = TranscodingUtil.byteArray2Long(Arrays.copyOfRange(fixedHeader, 2, 4));
        long timestamp = TranscodingUtil.byteArray2Long(Arrays.copyOfRange(fixedHeader, 4, 8));
        String ssrc = TranscodingUtil.byteArray2HexString(Arrays.copyOfRange(fixedHeader, 8, 12));

        if (version != RTP_VERSION) {
            return result;
        }
        if (csrcCount != 0 && packetLength < (4 * csrcCount + FIXED_RTP_HEADER_LENGTH)) {
            return result;
        }
        result.fluentPut("match", true)
                .fluentPut("payloadType", payloadType)
                .fluentPut("sequenceNumber", sequenceNumber)
                .fluentPut("timestamp", timestamp)
                .fluentPut("ssrc", ssrc);
        return result;
    }
}
