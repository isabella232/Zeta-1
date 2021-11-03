package com.ebay.dss.zds.rpc.message;

import com.ebay.dss.zds.common.JsonUtil;
import com.ebay.dss.zds.rpc.RpcAddress;
import com.ebay.dss.zds.rpc.RpcAddress.RpcAddressSource;
import com.ebay.dss.zds.rpc.listener.RpcMessageListener;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by tatian on 2020-09-07.
 */
public class RpcMessage<T extends Serializable> implements Serializable, Cloneable {

  public final long id = Math.abs(UUID.randomUUID().getLeastSignificantBits());
  protected final RpcAddress sender;
  protected volatile RpcAddress receiver;
  protected final T message;
  public final long createTime;
  private transient volatile long inBoundTime;
  private volatile boolean mustCheck = false;
  private volatile boolean checked = false;
  // this is only for in site message callback
  private transient volatile RpcMessageListener listener;
  private AtomicBoolean inTransport = new AtomicBoolean(false);

  public RpcMessage(T message, RpcAddress sender, RpcAddress receiver) {
    this.message = message;
    this.sender = sender;
    this.receiver = receiver;
    this.createTime = System.currentTimeMillis();
  }

  public RpcAddress getSender() {
    return sender;
  }

  public RpcAddress getReceiver() {
    return receiver;
  }

  public T getMessage() {
    return message;
  }

  @Override
  public String toString() {
    return toJson();
  }

  public String toJson() {
    return JsonUtil.GSON.toJson(this);
  }

  public boolean inTransport() {
    return this.inTransport.get();
  }

  public void setInTransport() {
    inTransport.set(true);
  }

  public boolean checked() {
    return this.checked;
  }

  public void markAsChecked() {
    this.checked = true;
  }

  /**
   * Only check the id and the sender, because the message could be copied and broadcast
   * so the receiver could be different.
   * Actually the id should be (or guarantee) global unique, so only id match can tells the result
   * **/
  public boolean sameSource(RpcMessage other) {
    return this.id == other.id && this.sender.equals(other.sender);
  }

  @Override
  public RpcMessage clone() {
    RpcMessage sc = null;
    try {
      sc = (RpcMessage) super.clone();
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
    }
    return sc;
  }

  public RpcMessage cloneWithNewReceiver(RpcAddress newAddress) {
    RpcMessage cloned = clone();
    cloned.receiver = newAddress;
    return cloned;
  }

  public void redirectToNewReceiver(RpcAddress newAddress) {
    this.receiver = newAddress;
  }

  public RpcAddressSource getRpcAddressSource() {
    return new RpcAddressSource(id, this.sender);
  }

  public long getInBoundTime() {
    return inBoundTime;
  }

  public void setInBoundTime(long inBoundTime) {
    this.inBoundTime = inBoundTime;
  }

  public void setListener(RpcMessageListener listener) {
    this.listener = listener;
  }

  public boolean isMustCheck() {
    return mustCheck;
  }

  public void setMustCheck(boolean mustCheck) {
    this.mustCheck = mustCheck;
  }

  public void callOutIgnore() {
    if (this.listener != null) {
      this.listener.onIgnore();
    }
  }


}
