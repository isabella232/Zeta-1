package com.ebay.dss.zds.rpc.message.streaming;

import com.ebay.dss.zds.rpc.RpcAddress;
import com.ebay.dss.zds.rpc.message.RpcOneWayMessage;

import java.io.Serializable;

/**
 * Created by tatian on 2020-09-13.
 */
public class RpcStreamingOneWayMessage <T extends Serializable>
        extends RpcOneWayMessage<T>
        implements RpcStreamingMessage {

  private final RpcAddress realReceiver;
  public final boolean forward;

  public RpcStreamingOneWayMessage(T message, RpcAddress sender, RpcAddress receiver, boolean forward) {
    super(message, sender, receiver.getStreamingRpcAddress());
    this.realReceiver = receiver;
    this.forward = forward;
  }

  public RpcAddress getRealTarget() {
    return this.realReceiver;
  }

}
