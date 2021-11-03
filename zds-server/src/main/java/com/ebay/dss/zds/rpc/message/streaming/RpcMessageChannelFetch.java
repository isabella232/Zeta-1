package com.ebay.dss.zds.rpc.message.streaming;

import com.ebay.dss.zds.rpc.RpcAddress;
import com.ebay.dss.zds.rpc.streaming.RpcMessageChannel;
import com.ebay.dss.zds.rpc.streaming.RpcStreamingPipe;

/**
 * Created by tatian on 2020-09-13.
 */
public class RpcMessageChannelFetch
        extends RpcStreamingRequestMessage<RequestContext, RpcMessageChannel> {

  public RpcMessageChannelFetch(RequestContext message, RpcAddress sender, RpcAddress receiver) {
    super(message, sender, receiver);
  }
}
