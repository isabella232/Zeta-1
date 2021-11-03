package com.ebay.dss.zds.rest.error;

public class SimpleErrorAction implements ErrorAction {

    private String message;

    public String getMessage() {
        return message;
    }

    public SimpleErrorAction setMessage(String message) {
        this.message = message;
        return this;
    }
}
