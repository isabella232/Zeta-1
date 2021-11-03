package com.ebay.dss.zds.interpreter.monitor.common;

import com.ebay.dss.zds.interpreter.ConfigurationManager.Cluster;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YarnUrl {
    public static final YarnUrl Hercules_Yarn_Url = new YarnUrl("https://hercules-lvs-rm-1.vip.ebay.com:50030",
            "https://hercules-lvs-rm-2.vip.ebay.com:50030");
    public static final YarnUrl Ares_Yarn_Url = new YarnUrl("https://ares-rm-1.vip.ebay.com:50030",
            "https://ares-rm-2.vip.ebay.com:50030");
    public static final YarnUrl Apollo_Yarn_Url = new YarnUrl("https://apollo-phx-rm-1.vip.ebay.com:50030",
            "https://apollo-phx-rm-2.vip.ebay.com:50030");

    private String url;
    private String url_backup;

    public YarnUrl(String url, String url_backup) {
        this.url = url;
        this.url_backup = url_backup;
    }

    public static YarnUrl get(Cluster cluster) {
        if (Cluster.apollophx.equals(cluster)) {
            return Apollo_Yarn_Url;
        } else if (Cluster.areslvs.equals(cluster)) {
            return Ares_Yarn_Url;
        } else if (Cluster.herculeslvs.equals(cluster)) {
            return Hercules_Yarn_Url;
        } else {
            throw new RuntimeException("unknown cluster: " + cluster);
        }
    }

    public static String findBackupUrl(String url) {
        Matcher matcher = Pattern.compile("-rm-\\d").matcher(url);
        if (matcher.find()) {
            String rm_d = matcher.group();
            switch (rm_d) {
                case "-rm-1":
                    return url.replace("-rm-1", "-rm-2");
                case "-rm-2":
                    return url.replace("-rm-2", "-rm-1");
                default:
                    return url;
            }
        } else {
            return url;
        }
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl_backup() {
        return url_backup;
    }

    public void setUrl_backup(String url_backup) {
        this.url_backup = url_backup;
    }

}
