package com.ebay.dss.zds.rpc.message.streaming;

import com.ebay.dss.zds.rpc.RpcAddress;
import com.ebay.dss.zds.rpc.message.RpcRequestMessage;

import java.io.Serializable;

/**
 * Created by tatian on 2020-09-13.
 */
public class RpcStreamingRequestMessage<T extends Serializable, V extends Serializable>
        extends RpcRequestMessage<T, V>
        implements RpcStreamingMessage {

  protected final RpcAddress realReceiver;

  public RpcStreamingRequestMessage(T message, RpcAddress sender, RpcAddress receiver) {
    super(message, sender, receiver.getStreamingRpcAddress());
    this.realReceiver = receiver;
  }

  public RpcAddress getRealTarget() {
    return this.realReceiver;
  }
}
