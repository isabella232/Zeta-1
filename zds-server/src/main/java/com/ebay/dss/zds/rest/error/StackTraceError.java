package com.ebay.dss.zds.rest.error;

public class StackTraceError extends StringMessageError {
    private Throwable cause;

    public StackTraceError(Throwable t) {
        super(t.getMessage());
        this.cause = t;
    }

    public Throwable getCause() {
        return this.cause;
    }

    public static StackTraceError fromThrowable(Throwable t) {
        return new StackTraceError(t);
    }
}
