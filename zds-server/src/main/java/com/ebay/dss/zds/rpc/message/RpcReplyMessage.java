package com.ebay.dss.zds.rpc.message;

import com.ebay.dss.zds.rpc.RpcAddress;

import java.io.Serializable;

/**
 * Created by tatian on 2020-09-07.
 */
public class RpcReplyMessage<T extends Serializable> extends RpcOneWayMessage<T> {

  protected boolean success = true;
  protected Throwable throwable;
  public final long sourceId;

  public RpcReplyMessage(long sourceId, T message, RpcAddress sender, RpcAddress receiver) {
    super(message, sender, receiver);
    this.sourceId = sourceId;
  }

  public void setFailure(Throwable throwable) {
    this.success = false;
    this.throwable = throwable;
  }

  public boolean isSuccess() {
    return this.success;
  }

  public Throwable getThrowable() {
    return this.throwable;
  }
}
