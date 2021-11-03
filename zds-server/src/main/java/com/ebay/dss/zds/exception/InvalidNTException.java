package com.ebay.dss.zds.exception;

public class InvalidNTException extends ApplicationBaseException {
    public InvalidNTException(String errorMsg) {
        super(ErrorCode.INVALID_NT, errorMsg);
    }
}
