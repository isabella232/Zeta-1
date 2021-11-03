package com.ebay.dss.zds.magic.exception;

/**
 * Created by tatian on 2021/4/15.
 */
public class UnWorkableNodeException extends RuntimeException {

  public UnWorkableNodeException(Throwable throwable) {
    super(throwable);
  }

  public UnWorkableNodeException(String cause) {
    super(cause);
  }
}

