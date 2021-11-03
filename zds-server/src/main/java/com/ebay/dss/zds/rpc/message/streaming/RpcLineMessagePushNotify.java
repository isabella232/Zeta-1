package com.ebay.dss.zds.rpc.message.streaming;

import com.ebay.dss.zds.rpc.RpcAddress;
import com.ebay.dss.zds.rpc.message.RpcOneWayMessage;
import com.ebay.dss.zds.rpc.streaming.RpcMessageChannel;
import com.ebay.dss.zds.rpc.streaming.RpcStreamingPipe;

/**
 * Created by tatian on 2020-09-16.
 */
public class RpcLineMessagePushNotify extends RpcOneWayMessage<LineMessageContext> {

  public RpcLineMessagePushNotify(LineMessageContext message, RpcAddress sender, RpcAddress receiver) {
    super(message, sender, receiver);
    this.setMustCheck(true);
    message.setHook(this::markAsChecked);
  }

}
