package cn.ninetailfox.util;

public class TranscodingUtil {
    private TranscodingUtil() {
    }

    public static String byteArray2HexString(byte[] source) {
        if (source == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < source.length; i++) {
            String hex = Integer.toHexString(source[i] & 0xff);
            if (hex.length() < 2) {
                sb.append("0");
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    public static byte[] hexString2byteArray(String source, boolean strict) {
        if (source == null) {
            return null;
        }
        if (source.length() % 2 == 1) {
            if (strict) {
                return null;
            }
            source = source.substring(0, source.length() - 1);
        }
        byte[] result = new byte[source.length() / 2];
        for (int i = 0; i < source.length(); i += 2) {
            result[i / 2] = (byte) Integer.parseInt(source.substring(i, i + 2), 16);
        }
        return result;
    }

}
