package com.ebay.dss.zds.rpc.message;

import com.ebay.dss.zds.rpc.RpcAddress;

import java.io.Serializable;

/**
 * Created by tatian on 2020-09-07.
 */
public class RpcCallSuccess<T extends Serializable> extends RpcReplyMessage<T>{

  public RpcCallSuccess(long sourceId, T message, RpcAddress sender, RpcAddress receiver) {
    super(sourceId, message, sender, receiver);
  }
}
