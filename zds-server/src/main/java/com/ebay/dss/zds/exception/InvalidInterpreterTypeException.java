package com.ebay.dss.zds.exception;

public class InvalidInterpreterTypeException extends ApplicationBaseException{

    public InvalidInterpreterTypeException(String message) {
        super(ErrorCode.INTERPRETER_INVALID_TYPE_EXCEPTION, message);
    }

    public InvalidInterpreterTypeException(Exception ex) {
        super(ErrorCode.INTERPRETER_INVALID_TYPE_EXCEPTION, ex.getMessage());
    }
}
