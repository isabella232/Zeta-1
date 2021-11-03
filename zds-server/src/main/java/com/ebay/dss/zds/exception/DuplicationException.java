package com.ebay.dss.zds.exception;

/**
 * Created by wenliu2 on 4/3/18.
 */
public class DuplicationException extends ApplicationBaseException {
  public DuplicationException(String message) {
    super(ErrorCode.DUPLICATION, message);
  }
}
