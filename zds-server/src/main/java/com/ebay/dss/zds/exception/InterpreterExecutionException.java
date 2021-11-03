package com.ebay.dss.zds.exception;

import com.ebay.dss.zds.websocket.notebook.dto.ExecuteCodeError;

/**
 * Used in interpreter code execution stage.
 */
public class InterpreterExecutionException extends ApplicationBaseException {

    private ExecuteCodeError error;

    public ExecuteCodeError getError() {
        return error;
    }

    public InterpreterExecutionException(ErrorCode errorCode, ExecuteCodeError error) {
        super(errorCode);
        this.error = error;
    }

    public InterpreterExecutionException(ErrorCode errorCode, ExecuteCodeError error, Throwable cause) {
        super(errorCode, cause);
        this.error = error;
    }

    @Override
    public String getMessage() {
        return String.format("execution[%s][%s][%s] %s",
                error.getNotebookId(), error.getJobId(), error.getReqId(),
                error.getErrors().toString());
    }
}
