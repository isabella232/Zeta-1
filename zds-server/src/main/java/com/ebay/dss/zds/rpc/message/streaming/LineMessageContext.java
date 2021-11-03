package com.ebay.dss.zds.rpc.message.streaming;

import com.ebay.dss.zds.rpc.streaming.RpcMessageChannel;

import java.io.Serializable;

/**
 * Created by tatian on 2020-09-26.
 */
public class LineMessageContext implements Serializable {

  private transient RpcMessageChannel pipe;
  private transient Runnable hook;

  public LineMessageContext() {

  }

  public LineMessageContext(RpcMessageChannel pipe) {
    this.pipe = pipe;
  }

  public LineMessageContext(RpcMessageChannel pipe, Runnable hook) {
    this.pipe = pipe;
    this.hook = hook;
  }

  public RpcMessageChannel getPipe() {
    try {
      return pipe;
    } finally {
      if (hook != null) hook.run();
    }
  }

  public void setPipe(RpcMessageChannel pipe) {
    this.pipe = pipe;
  }

  public void setHook(Runnable hook) {
    this.hook = hook;
  }
}
