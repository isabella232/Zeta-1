package com.ebay.dss.zds.rpc.exception;

import com.ebay.dss.zds.rpc.RpcAddress;

/**
 * Created by tatian on 2020-09-07.
 */
public class RpcEndpointNotFoundException extends RuntimeException {

  private RpcAddress receiver;

  public RpcEndpointNotFoundException(RpcAddress receiver, String message) {
    super(message);
    this.receiver = receiver;
  }
}
