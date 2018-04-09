package com.neil.simplerpc.core.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author neil
 */
public final class HostUtil {

    public static String getLocalHost() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "127.0.0.1";
        }
    }
}
