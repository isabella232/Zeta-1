package com.ebay.dss.zds.common;

import org.slf4j.MDC;

public class LogUtil {

    private LogUtil() {
    }

    public static String displayableSecret(String secret, int head, int tail, int threshold) {
        if (secret.length() < threshold || tail < 0 ||
                head > secret.length() || head + tail > secret.length() / 2)
            return "***";

        return secret.substring(0, head) + "***" +
                secret.substring(secret.length() - head, secret.length());
    }


}
