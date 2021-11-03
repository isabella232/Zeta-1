package com.ebay.dss.zds.rpc.exception;

import com.ebay.dss.zds.rpc.RpcAddress;

/**
 * Created by tatian on 2020-09-07.
 */
public class RpcEndpointStoppedException extends RuntimeException {

  private RpcAddress receiver;

  public RpcEndpointStoppedException(RpcAddress receiver, String message) {
    super(message);
    this.receiver = receiver;
  }
}
