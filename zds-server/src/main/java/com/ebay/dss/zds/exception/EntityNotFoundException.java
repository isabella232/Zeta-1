package com.ebay.dss.zds.exception;

/**
 * Created by wenliu2 on 4/3/18.
 */
public class EntityNotFoundException extends ApplicationBaseException {
  public EntityNotFoundException(String message) {
    super(ErrorCode.NOT_FOUND, message);
  }
}
