package com.ebay.dss.zds.exception;

import com.ebay.dss.zds.model.ZetaStatus;

/**
 * Used in interpreter instantiation/destruction/self-monitor stage.
 */
public class InterpreterServiceException extends ApplicationBaseException {

    private ZetaStatus zetaStatus;
    private String noteId;

    public ZetaStatus getZetaStatus() {
        return zetaStatus;
    }

    public String getNoteId() {
        return noteId;
    }

    public InterpreterServiceException(ErrorCode errorCode, ZetaStatus zetaStatus, String noteId, String message) {
        this(errorCode, zetaStatus, noteId, message, null);
    }

    public InterpreterServiceException(ErrorCode errorCode, ZetaStatus zetaStatus, String noteId, Throwable cause) {
        this(errorCode, zetaStatus, noteId, cause.getMessage(), cause);
    }

    public InterpreterServiceException(ErrorCode errorCode, ZetaStatus zetaStatus, String noteId, String message, Throwable cause) {
        super(errorCode, message, cause);
        this.zetaStatus = zetaStatus;
        this.noteId = noteId;
    }

    @Override
    public String getMessage() {
        return String.format("notebook[%s][%s] %s", getNoteId(), getZetaStatus(), super.getMessage());
    }
}
