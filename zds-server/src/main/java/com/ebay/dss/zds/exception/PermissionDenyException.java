package com.ebay.dss.zds.exception;

public class PermissionDenyException extends ApplicationBaseException {
  public PermissionDenyException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
