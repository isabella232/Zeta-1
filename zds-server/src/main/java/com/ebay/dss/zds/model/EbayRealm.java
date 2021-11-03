package com.ebay.dss.zds.model;

public enum EbayRealm {
    CORP("CORP.EBAY.COM"),
    APD("APD.EBAY.COM"),
    PROD("PROD.EBAY.COM"),
    ;

    public final String value;

    EbayRealm(String value) {
        this.value = value;
    }
}
