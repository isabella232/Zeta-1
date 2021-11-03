package com.ebay.dss.zds.serverconfig;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "cipher.rsa")
public class RSACipherProperties {

    private String privateKey;
    private String publicKey;

    public String getPrivateKey() {
        return privateKey;
    }

    public RSACipherProperties setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
        return this;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public RSACipherProperties setPublicKey(String publicKey) {
        this.publicKey = publicKey;
        return this;
    }
}
