package com.ebay.dss.zds.rest.error;

import com.ebay.dss.zds.model.ZetaStatus;

public interface ErrorContext {

    static ErrorContext of(Object obj) {
        return new SimpleErrorContext()
                .setObject(obj);
    }

    static ErrorContext of(String notebookId, ZetaStatus status) {
        return new NotebookStatusContext()
                .setNotebookId(notebookId)
                .setZetaStatus(status);
    }
}
