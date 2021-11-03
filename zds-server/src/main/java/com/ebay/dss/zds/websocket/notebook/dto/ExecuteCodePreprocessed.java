package com.ebay.dss.zds.websocket.notebook.dto;

import java.io.Serializable;
import java.util.List;

/**
 * Created by wenliu2 on 5/4/18.
 */
public class ExecuteCodePreprocessed implements Serializable {
  private String notebookId;
  private String jobId;
  private String reqId;
  private List<CodeWithSeq> codes;

  public ExecuteCodePreprocessed(String notebookId, String jobId, String reqId, List<CodeWithSeq> codes) {
    this.notebookId = notebookId;
    this.jobId = jobId;
    this.reqId = reqId;
    this.codes = codes;
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

  public List<CodeWithSeq> getCodes() {
    return codes;
  }

  public void setCodes(List<CodeWithSeq> codes) {
    this.codes = codes;
  }

  public String getReqId() {
    return reqId;
  }

  public void setReqId(String reqId) {
    this.reqId = reqId;
  }
}
