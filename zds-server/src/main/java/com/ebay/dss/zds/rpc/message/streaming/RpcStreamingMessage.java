package com.ebay.dss.zds.rpc.message.streaming;

import com.ebay.dss.zds.rpc.RpcAddress;

/**
 * Created by tatian on 2020-09-13.
 */
public interface RpcStreamingMessage {

  RpcAddress getRealTarget();
  RpcAddress getSender();


}
