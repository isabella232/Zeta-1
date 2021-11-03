package com.ebay.dss.zds.rpc.exception;

/**
 * Created by tatian on 2020-09-07.
 */
public class RpcMessageIgnoredException extends RuntimeException {

  public RpcMessageIgnoredException(String message) {
    super(message);
  }

}
