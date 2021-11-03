package com.ebay.dss.zds.websocket.notebook.dto;

import java.io.Serializable;

public class SessionExpiredMsg implements Serializable {
    private String notebookId;
    private String reason;
    public SessionExpiredMsg(String notebookId,String reason){
        this.notebookId=notebookId;
        this.reason=reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getNotebookId() {
        return notebookId;
    }

    public void setNotebookId(String notebookId) {
        this.notebookId = notebookId;
    }
}
