package com.ebay.dss.zds.model;

import java.io.Serializable;

public class FileStorageResponse<T> implements Serializable {

  public static final long serialVersionUID = 42L;

  public static final int SUCCESS_CODE = 200;
  public static final int FAIL_CODE = 500;
  public static final int REDIRECT_CODE = 302;
  public static final FileStorageResponse<String> SUCCESS = new FileStorageResponse<>(null);
  public static final FileStorageResponse<String> FAIL = new FileStorageResponse<>(FAIL_CODE, null);
  public static final FileStorageResponse<String> REDIRECT = new FileStorageResponse<>(REDIRECT_CODE, null);

  private int code;
  private String msg;
  private T content;

  public FileStorageResponse(int code, String msg) {
    this.code = code;
    this.msg = msg;
  }

  public FileStorageResponse(T content) {
    this.code = SUCCESS_CODE;
    this.content = content;
  }

  public FileStorageResponse(int code, String msg, T content) {
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

  public boolean isSuccess() {
    return code == SUCCESS_CODE ? true : false;
  }

  @Override
  public String toString() {
    return "FileStorageResponse [code=" + code + ", msg=" + msg + ", content=" + content + "]";
  }
}
