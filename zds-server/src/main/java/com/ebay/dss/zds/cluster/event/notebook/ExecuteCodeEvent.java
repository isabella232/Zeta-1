package com.ebay.dss.zds.cluster.event.notebook;

import com.ebay.dss.zds.rpc.RpcAddress;
import com.ebay.dss.zds.rpc.message.streaming.RequestContext;
import com.ebay.dss.zds.rpc.message.streaming.RpcMessageChannelFetch;
import com.ebay.dss.zds.websocket.notebook.dto.ExecuteCodeReq;

/**
 * Created by tatian on 2021-02-05.
 */
public class ExecuteCodeEvent extends RpcMessageChannelFetch {

  public ExecuteCodeEvent(RequestContext<ExecuteCodeReq> message, RpcAddress sender, RpcAddress receiver) {
    super(message, sender, receiver);
  }
}
