package com.ebay.dss.zds.rpc.message.streaming;

import com.ebay.dss.zds.rpc.RpcAddress;

/**
 * Created by tatian on 2020-09-13.
 */
public class RpcStreamingBlock extends RpcStreamingOneWayMessage<StreamingBlock> {

  // the source rpc streaming request id
  public final long streamingId;
  public final Class messageType;

  public RpcStreamingBlock(Class messageType, long streamingId, StreamingBlock message, RpcAddress sender, RpcAddress receiver, boolean forward) {
    super(message, sender, receiver, forward);
    this.streamingId = streamingId;
    this.messageType = messageType;
  }
}
