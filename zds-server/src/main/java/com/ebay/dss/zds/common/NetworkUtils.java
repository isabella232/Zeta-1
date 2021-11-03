package com.ebay.dss.zds.common;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by tatian on 2019-06-20.
 */
public class NetworkUtils {

    public static String getHostIP() {
        try {
            InetAddress ia = InetAddress.getLocalHost();
            return ia.getHostAddress();
        } catch (UnknownHostException ex) {
            return "Unknown host";
        }
    }

    public static String getHostName() {
        try {
            InetAddress ia = InetAddress.getLocalHost();
            return ia.getHostName();
        } catch (UnknownHostException ex) {
            return "Unknown host";
        }
    }
}
