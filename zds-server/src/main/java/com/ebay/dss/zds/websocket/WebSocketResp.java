package com.ebay.dss.zds.websocket;

import java.io.Serializable;

/**
 * Created by wenliu2 on 4/11/18.
 */
public class WebSocketResp<T> implements Serializable {
  public enum OP{
    INTERNAL_ERROR,
    NB_CODE_INVALID_NOTEBOOK_STATUS,
    NB_CODE_EXECUTE_ERROR,
    NB_CODE_STATEMENT_SUCCESS,
    NB_CODE_JOB_READY,
    NB_CODE_JOB_DONE,
    NB_CODE_DUMP_DONE,
    NB_CODE_JOB_CANCELLED,
    NB_CODE_JOB_CANCELLING,
    NB_CODE_PREPROCESSED,
    NB_CODE_STATEMENT_START,
    NB_CODE_STATEMENT_PROGRESS,
    NB_CODE_SESSION_EXPIRED,
    NB_VAR_REFRESH,
    NB_VAR_REFRESH_FAILED,
    GREETING,
    SESSIONS,
    SESSION_ACTIVE,
    CONNECTION_SUCCESS,
    CONNECTION_ERROR,
    CONNECTION_ABORT,
    CONNECTION_PROGRESS,
    DISCONNECTION_SUCCESS,
    DISCONNECTION_ERROR,
    CANCEL_SUCCESS,
    CANCEL_ERROR,
    RECOVER_TRIED
  }

  private String op;
  private T data;

  public WebSocketResp(OP op, T data) {
    this.op = op.name();
    this.data = data;
  }

  public static <T1> WebSocketResp<T1> get(OP op, T1 data){
    return new WebSocketResp(op, data);
  }

  public String getOp() {
    return op;
  }

  public void setOp(String op) {
    this.op = op;
  }

  public T getData() {
    return data;
  }

  public void setData(T data) {
    this.data = data;
  }


}
