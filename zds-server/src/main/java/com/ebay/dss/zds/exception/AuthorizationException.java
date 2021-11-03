package com.ebay.dss.zds.exception;

public class AuthorizationException extends ApplicationBaseException {

  public AuthorizationException(String message) {
    super(ErrorCode.AUTHORIZATION_EXCEPTION, message);
  }
}
