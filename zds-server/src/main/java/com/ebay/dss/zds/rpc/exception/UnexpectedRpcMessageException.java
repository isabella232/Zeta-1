package com.ebay.dss.zds.rpc.exception;

/**
 * Created by tatian on 2020-09-13.
 */
public class UnexpectedRpcMessageException extends IllegalArgumentException{

  public UnexpectedRpcMessageException(String message) {
    super(message);
  }
}
