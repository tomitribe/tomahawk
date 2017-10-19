/*
 * Tomitribe Confidential
 *
 * Copyright Tomitribe Corporation. 2016
 *
 * The source code for this program is not published or otherwise divested of its trade secrets, irrespective of what has been
 * deposited with the U.S. Copyright Office.
 *
 */
package com.tomitribe.tomahawk;

public class TomahawkPrinter {
    public static void printLogLine(String label, byte[] encrypted, byte[] preMaster) {
        String logLine = String.format("%s %s %s", label, toHexString(encrypted), toHexString(preMaster));
        System.out.println(logLine);
    }

    public static void printCLIENT_RANDOM(byte[] clientRandom, byte[] masterSecret) {
        String logLine = String.format("CLIENT_RANDOM %s %s", toHexString(clientRandom), toHexString(masterSecret));
        System.out.println(logLine);
    }

    private static String toHexString(byte[] encoded) {
        StringBuilder hexString = new StringBuilder();
        for (int position = 0; position < encoded.length; position++) {
            int currentByte = encoded[position] & 0xFF;
            hexString.append(currentByte < 0x10 ? "0" : "");
            hexString.append(Integer.toHexString(currentByte));
        }
        return hexString.toString();
    }
}
