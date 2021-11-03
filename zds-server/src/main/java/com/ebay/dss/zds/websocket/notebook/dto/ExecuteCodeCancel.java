package com.ebay.dss.zds.websocket.notebook.dto;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

public class ExecuteCodeCancel implements Serializable {
    @NotBlank
    private String notebookId;
    @NotBlank
    private String userName;

    private String jobId;

    public String getNotebookId() {
        return notebookId;
    }

    public void setNotebookId(String notebookId) {
        this.notebookId = notebookId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }
}
