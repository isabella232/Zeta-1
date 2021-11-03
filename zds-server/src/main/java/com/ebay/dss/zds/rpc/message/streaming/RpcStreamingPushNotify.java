package com.ebay.dss.zds.rpc.message.streaming;

import com.ebay.dss.zds.rpc.RpcAddress;
import com.ebay.dss.zds.rpc.message.RpcOneWayMessage;

/**
 * Created by tatian on 2020-09-16.
 */
public class RpcStreamingPushNotify extends RpcOneWayMessage<PushContext> {

  public RpcStreamingPushNotify(PushContext message, RpcAddress sender, RpcAddress receiver) {
    super(message, sender, receiver);
    this.setMustCheck(true);
    message.setHook(this::markAsChecked);
  }
}
