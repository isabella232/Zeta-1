package com.ebay.dss.zds.rest.error;

public interface ErrorAction {

    static ErrorAction of(String message) {
        return new SimpleErrorAction().setMessage(message);
    }
}
