package com.ebay.dss.zds.exception;

/**
 * Created by wenliu2 on 4/11/18.
 */
public class InvalidInputException extends ApplicationBaseException{

  public InvalidInputException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }

  public InvalidInputException(ErrorCode errorCode, String message, Throwable t) {
    super(errorCode, message, t);
  }
}
