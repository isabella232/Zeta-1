package com.ebay.dss.zds.rpc.message;

import com.ebay.dss.zds.rpc.RpcAddress;

import java.io.Serializable;

/**
 * Created by tatian on 2020-09-07.
 */
public class RpcOneWayMessage<T extends Serializable> extends RpcMessage<T> {

  public RpcOneWayMessage(T message, RpcAddress sender, RpcAddress receiver) {
   super(message, sender, receiver);
  }
}
