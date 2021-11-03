package com.ebay.dss.zds.websocket.notebook.dto;

import com.ebay.dss.zds.interpreter.monitor.modle.Status;

import java.io.Serializable;

/**
 * Created by wenliu2 on 5/4/18.
 */
public class ExecuteCodeProgress implements Serializable {
  private String notebookId;
  private String jobId;
  private CodeWithSeq code;
  private Status status;
  private String reqId;
  private String sparkJobUrl;

  public ExecuteCodeProgress(String notebookId, String jobId, String reqId) {
    this.notebookId = notebookId;
    this.jobId = jobId;
    this.reqId = reqId;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
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

  public CodeWithSeq getCode() {
    return code;
  }

  public void setCode(CodeWithSeq code) {
    this.code = code;
  }

  public String getReqId() {
    return reqId;
  }

  public void setReqId(String reqId) {
    this.reqId = reqId;
  }

  public String getSparkJobUrl() {
    return sparkJobUrl;
  }

  public void setSparkJobUrl(String sparkJobUrl) {
    this.sparkJobUrl = sparkJobUrl;
  }
}
