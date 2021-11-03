package com.ebay.dss.zds.service.metatable;

import java.io.Serializable;

public class MetaTableResponse<T> implements Serializable {

  public static final long serialVersionUID = 42L;

  public static final int SUCCESS_CODE = 200;
  public static final int FORBIDDEN_CODE = 403;
  public static final int FAIL_CODE = 500;
  public static final MetaTableResponse<String> SUCCESS
      = new MetaTableResponse<>(SUCCESS_CODE, null);
  public static final MetaTableResponse<String> FAIL
      = new MetaTableResponse<>(FAIL_CODE, null);

  private int code;
  private String msg;
  private T content;

  public MetaTableResponse(int code, String msg) {
    this.code = code;
    this.msg = msg;
  }

  public MetaTableResponse(int code, T content) {
    this.code = code;
    this.content = content;
  }

  public MetaTableResponse(int code, String msg, T content) {
    this.code = code;
    this.msg = msg;
    this.content = content;
  }

  public int getCode() {
    return code;
  }

  public void setCode(int code) {
    this.code = code;
  }

  public String getMsg() {
    return msg;
  }

  public void setMsg(String msg) {
    this.msg = msg;
  }

  public T getContent() {
    return content;
  }

  public void setContent(T content) {
    this.content = content;
  }

  @Override
  public String toString() {
    return "MetaTableResponse [code=" + code + ", msg=" + msg + ", content=" + content + "]";
  }

}
