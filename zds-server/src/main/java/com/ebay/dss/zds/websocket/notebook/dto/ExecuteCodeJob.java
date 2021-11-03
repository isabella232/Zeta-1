package com.ebay.dss.zds.websocket.notebook.dto;

import java.io.Serializable;

/**
 * Created by wenliu2 on 5/4/18.
 */
public class ExecuteCodeJob implements Serializable {
  private String notebookId;
  private String jobId;
  private String reqId;


  public ExecuteCodeJob(String notebookId, String jobId, String reqId) {
    this.notebookId = notebookId;
    this.jobId = jobId;
    this.reqId = reqId;
  }

  public String getNotebookId() {
    return notebookId;
  }

  public void setNotebookId(String notebookId) {
    this.notebookId = notebookId;
  }

  public String getJobId() {
    return jobId;
  }

  public void setJobId(String jobId) {
    this.jobId = jobId;
  }

  public String getReqId() {
    return reqId;
  }

  public void setReqId(String reqId) {
    this.reqId = reqId;
  }
}
