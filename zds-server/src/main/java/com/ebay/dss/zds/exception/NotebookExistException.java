package com.ebay.dss.zds.exception;

public class NotebookExistException extends ApplicationBaseException {
    public NotebookExistException(String message) {
        super(ErrorCode.EXIST_NOTEBOOK, message);
    }
}
