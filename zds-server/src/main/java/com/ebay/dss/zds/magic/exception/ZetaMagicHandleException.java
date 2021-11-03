package com.ebay.dss.zds.magic.exception;

/**
 * Created by tatian on 2020-10-30.
 */
public class ZetaMagicHandleException extends RuntimeException {

  public ZetaMagicHandleException(Throwable throwable) {
    super(throwable);
  }

  public ZetaMagicHandleException(String message) {
    super(message);
  }
}
