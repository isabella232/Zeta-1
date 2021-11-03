package com.ebay.dss.zds.magic.exception;

/**
 * Created by tatian on 2020-10-30.
 */
public class ZetaVariableInjectionException extends RuntimeException {

  public ZetaVariableInjectionException(Throwable throwable) {
    super(throwable);
  }

  public ZetaVariableInjectionException(String cause) {
    super(cause);
  }
}
