package com.ebay.dss.zds.exception;

public class UnknownClusterException extends ApplicationBaseException {

    public UnknownClusterException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
