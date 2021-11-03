package com.ebay.dss.zds.exception;

/**
 * Created by wenliu2 on 4/16/18.
 */
public class IllegalStatusException extends ApplicationBaseException{
  public IllegalStatusException(String message) {
    super(ErrorCode.ILLEGAL_STATUS, message);
  }
}
