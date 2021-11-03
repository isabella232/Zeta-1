package com.ebay.dss.zds.exception;

/**
 * Created by tatian on 2018/4/23.
 */
public class InterpreterException extends ApplicationBaseException{
    public InterpreterException(String message) {
        super(ErrorCode.INTERPRETER_GENERIC_EXCEPTION, message);
    }

    public InterpreterException(Exception ex) {
        super(ErrorCode.INTERPRETER_GENERIC_EXCEPTION, ex.getMessage(), ex);
    }

    public InterpreterException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public InterpreterException(ErrorCode errorCode, Exception ex) {
        super(errorCode, ex.getMessage(), ex);
    }

    public InterpreterException(ErrorCode errorCode, String message, Exception ex) {
        super(errorCode, message, ex);
    }
}
