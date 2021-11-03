package com.ebay.dss.zds.exception;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.validation.constraints.NotNull;

/**
 * Created by wenliu2 on 4/3/18.
 */
public class ApplicationBaseException extends RuntimeException {

    private final ErrorCode errorCode;
    private transient ExceptionListener exceptionListener;

    public ApplicationBaseException(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    public ApplicationBaseException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ApplicationBaseException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ApplicationBaseException(ErrorCode errorCode, Throwable cause) {
        super(cause);
        this.errorCode = errorCode;

    }

    public ApplicationBaseException(ErrorCode errorCode, String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return this.errorCode;
    }

    @NotNull
    @JsonIgnore
    public ExceptionListener getExceptionListener() {
        if (exceptionListener == null) {
            exceptionListener = new ExceptionListener(this) {
                @Override
                public void onExceptionCaught() {
                    super.onExceptionCaught();
                }
            };
        }
        return exceptionListener;
    }

    public void setExceptionListener(ExceptionListener exceptionListener) {
        this.exceptionListener = exceptionListener;
    }
}
