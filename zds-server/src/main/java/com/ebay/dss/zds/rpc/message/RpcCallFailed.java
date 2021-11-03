package com.ebay.dss.zds.rpc.message;

import com.ebay.dss.zds.rpc.RpcAddress;

/**
 * Created by tatian on 2020-09-07.
 */
public class RpcCallFailed extends RpcReplyMessage<Throwable> {

  public RpcCallFailed(long sourceId, Throwable throwable, RpcAddress sender, RpcAddress receiver) {
    super(sourceId, throwable, sender, receiver);
    setFailure(throwable);
  }
}
