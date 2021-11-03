package com.ebay.dss.zds.rpc.exception;

/**
 * Created by tatian on 2020-09-08.
 */
public class RpcSendFailException extends RuntimeException {

  public RpcSendFailException(Throwable cause) {
    super(cause);
  }

  public RpcSendFailException(String cause) {
    super(cause);
  }
}
