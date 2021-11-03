package com.ebay.dss.zds.rpc.message.streaming;

import com.ebay.dss.zds.rpc.RpcAddress;
import com.ebay.dss.zds.rpc.streaming.RpcStreamingPipe;

/**
 * Created by tatian on 2020-09-13.
 */
public class RpcStreamingFetch
        extends RpcStreamingRequestMessage<RequestContext, RpcStreamingPipe> {

  public RpcStreamingFetch(RequestContext message, RpcAddress sender, RpcAddress receiver) {
    super(message, sender, receiver);
  }
}
