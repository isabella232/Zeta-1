package com.ebay.dss.zds.magic.exception;

/**
 * Created by tatian on 2021/4/15.
 */
public class ZetaWorkflowParseException extends RuntimeException {

  public ZetaWorkflowParseException(Throwable throwable) {
    super(throwable);
  }

  public ZetaWorkflowParseException(String cause) {
    super(cause);
  }
}
