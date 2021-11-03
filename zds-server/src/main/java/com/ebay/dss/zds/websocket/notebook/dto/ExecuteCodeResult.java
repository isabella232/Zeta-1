package com.ebay.dss.zds.websocket.notebook.dto;

import java.io.Serializable;

/**
 * Created by wenliu2 on 5/4/18.
 */
public class ExecuteCodeResult implements Serializable {
  private String notebookId;
  private String jobId;
  private CodeWithSeq code;
  private String result;
  private String reqId;
  private Long startDt;
  private Long endDt;

  public ExecuteCodeResult(String notebookId, String jobId, String reqId) {
    this.notebookId = notebookId;
    this.jobId = jobId;
    this.reqId = reqId;
  }

  public String getResult() {
    return result;
  }

  public void setResult(String result) {
    this.result = result;
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

  public Long getStartDt() {
    return startDt;
  }

  public void setStartDt(Long startDt) {
    this.startDt = startDt;
  }

  public Long getEndDt() {
    return endDt;
  }

  public void setEndDt(Long endDt) {
    this.endDt = endDt;
  }
}
