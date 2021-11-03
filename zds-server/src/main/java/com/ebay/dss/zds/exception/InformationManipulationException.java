package com.ebay.dss.zds.exception;

public class InformationManipulationException extends ApplicationBaseException {

    public final String subject;
    public final String action;

    public InformationManipulationException(String subject, String action, Throwable cause) {
        super(ErrorCode.MALFORMED_INFORMATION, cause);
        this.subject = subject;
        this.action = action;
    }
}
