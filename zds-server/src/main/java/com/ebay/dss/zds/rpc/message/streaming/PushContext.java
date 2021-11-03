package com.ebay.dss.zds.rpc.message.streaming;

import com.ebay.dss.zds.rpc.streaming.RpcStreamingPipe;

import java.io.Serializable;

/**
 * Created by tatian on 2020-09-16.
 */
public class PushContext implements Serializable {

  private transient RpcStreamingPipe pipe;
  private transient Runnable hook;

  public PushContext() {

  }

  public PushContext(RpcStreamingPipe pipe) {
    this.pipe = pipe;
  }

  public PushContext(RpcStreamingPipe pipe, Runnable hook) {
    this.pipe = pipe;
    this.hook = hook;
  }

  public RpcStreamingPipe getPipe() {
    try {
      return pipe;
    } finally {
      if (hook != null) hook.run();
    }
  }

  public void setPipe(RpcStreamingPipe pipe) {
    this.pipe = pipe;
  }

  public void setHook(Runnable hook) {
    this.hook = hook;
  }
}
