package com.ebay.dss.zds.rest.error;

import com.ebay.dss.zds.model.ZetaStatus;

public class NotebookStatusContext implements ErrorContext {

    private String notebookId;
    private ZetaStatus zetaStatus;

    public String getNotebookId() {
        return notebookId;
    }

    public NotebookStatusContext setNotebookId(String notebookId) {
        this.notebookId = notebookId;
        return this;
    }

    public ZetaStatus getZetaStatus() {
        return zetaStatus;
    }

    public NotebookStatusContext setZetaStatus(ZetaStatus zetaStatus) {
        this.zetaStatus = zetaStatus;
        return this;
    }
}
