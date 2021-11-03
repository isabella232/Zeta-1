package com.ebay.dss.zds.rpc.message.streaming;

import com.ebay.dss.zds.rpc.RpcAddress;
import com.ebay.dss.zds.rpc.message.RpcReplyMessage;

import java.io.Serializable;

/**
 * Created by tatian on 2020-09-13.
 */
public class RpcStreamingReplyMessage<T extends Serializable>
        extends RpcReplyMessage<T>
        implements RpcStreamingMessage {

  private final RpcAddress realReceiver;

  public RpcStreamingReplyMessage(long sourceId, T message, RpcAddress sender, RpcAddress receiver) {
    super(sourceId, message, sender, receiver.getStreamingRpcAddress());
    this.realReceiver = receiver;
  }

  public RpcAddress getRealTarget() {
    return this.realReceiver;
  }
}
