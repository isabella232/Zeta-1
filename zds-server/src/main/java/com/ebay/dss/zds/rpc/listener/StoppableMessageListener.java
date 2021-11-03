package com.ebay.dss.zds.rpc.listener;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by tatian on 2020-10-10.
 */
public abstract class StoppableMessageListener<T extends Serializable> implements RpcMessageListener<T> {

  private final AtomicBoolean canStop = new AtomicBoolean(false);

  public boolean canStop() {
    return this.canStop.get();
  }

  public void stop() {
    this.canStop.compareAndSet(false, true);
  }

}
