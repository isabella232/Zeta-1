package com.ebay.dss.zds.websocket.notebook.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by wenliu2 on 5/4/18.
 */
public class ExecuteCodeError implements Serializable {
  private String notebookId;
  private List<String> errors;
  private String jobId;
  private String reqId;
  private CodeWithSeq code;

  public ExecuteCodeError(String notebookId, String reqId) {
    this.notebookId = notebookId;
    this.errors = new ArrayList<>();
    this.reqId = reqId;
  }

  public List<String> getErrors() {
    return errors;
  }

  public void addError(String error) {
    this.errors.add(error);
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

  public void setErrors(List<String> errors) {
    this.errors = errors;
  }

  public String getReqId() {
    return reqId;
  }

  public void setReqId(String reqId) {
    this.reqId = reqId;
  }
}
