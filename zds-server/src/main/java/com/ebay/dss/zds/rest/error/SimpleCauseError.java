package com.ebay.dss.zds.rest.error;

import com.ebay.dss.zds.exception.InterpreterExecutionException;
import com.ebay.dss.zds.exception.InterpreterServiceException;

import java.util.HashMap;
import java.util.Map;

public class SimpleCauseError extends StringMessageError {

    private Object cause;

    @Deprecated
    public SimpleCauseError(String message, Object cause) {
        super(message);
        this.cause = cause;
        super.setContext(ErrorContext.of(cause));
    }

    public SimpleCauseError(String message, Object cause, ErrorContext context) {
        super(message);
        this.cause = cause;
        super.setContext(context);
    }

    public Object getCause() {
        return cause;
    }

    public void setCause(Object cause) {
        this.cause = cause;
    }

    public static SimpleCauseError from(InterpreterServiceException ise) {
        Map<String, Object> cause = new HashMap<>();
        cause.put("zetaStatus", ise.getZetaStatus());
        cause.put("noteId", ise.getNoteId());
        return new SimpleCauseError(ise.getMessage(), cause, ErrorContext.of(ise.getNoteId(), ise.getZetaStatus()));
    }

    public static SimpleCauseError from(InterpreterExecutionException iee) {
        return new SimpleCauseError(iee.getMessage(), iee.getError());
    }

}
