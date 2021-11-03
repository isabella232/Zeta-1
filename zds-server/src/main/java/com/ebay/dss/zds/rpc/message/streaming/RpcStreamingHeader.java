package com.ebay.dss.zds.rpc.message.streaming;

import com.ebay.dss.zds.rpc.RpcAddress;

/**
 * Created by tatian on 2020-09-13.
 */
public class RpcStreamingHeader extends RpcStreamingOneWayMessage<StreamingHeader> {

  public final long nextBlockId;
  public final Class messageType;

  public RpcStreamingHeader(Class messageType, long streamingId, long nextBlockId, RpcAddress sender, RpcAddress receiver, boolean forward) {
    super(new StreamingHeader(streamingId), sender, receiver, forward);
    this.nextBlockId = nextBlockId;
    this.messageType = messageType;
  }
}
