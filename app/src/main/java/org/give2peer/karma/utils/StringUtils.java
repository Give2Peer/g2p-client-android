package org.give2peer.karma.utils;

/**
 *
 */
public class StringUtils
{
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    /**
     * Used to read certificates.
     *
     * @param bytes to read from
     * @return the hex string representation of `bytes`.
     */
    public static String bytesToHex(byte[] bytes)
    {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String httpsToHttp(String url)
    {
        return url.replaceFirst("^https", "http");
    }
}
