package com.ebay.dss.zds.model;

public class KylinReadableReq {

    private boolean ssl = false;
    private String host;
    private String user;
    private String password;

    public boolean isSsl() {
        return ssl;
    }

    public KylinReadableReq setSsl(boolean ssl) {
        this.ssl = ssl;
        return this;
    }

    public String getHost() {
        return host;
    }

    public KylinReadableReq setHost(String host) {
        this.host = host;
        return this;
    }

    public String getUser() {
        return user;
    }

    public KylinReadableReq setUser(String user) {
        this.user = user;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public KylinReadableReq setPassword(String password) {
        this.password = password;
        return this;
    }
}
