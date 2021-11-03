package com.ebay.dss.zds.rpc.exception;

import com.ebay.dss.zds.rpc.RpcAddress;

/**
 * Created by tatian on 2020-09-09.
 */
public class RpcCallCanceledException extends RuntimeException {

  private RpcAddress receiver;

  public RpcCallCanceledException(RpcAddress receiver, String message) {
    super(message);
    this.receiver = receiver;
  }
}
