package com.ebay.dss.zds.rpc.listener.streaming;

import com.ebay.dss.zds.rpc.listener.RpcMessageListener;
import com.ebay.dss.zds.rpc.message.streaming.LineMessage;

import java.io.IOException;

/**
 * Created by tatian on 2020-09-26.
 */
public interface RpcLineMessageListener extends RpcMessageListener<LineMessage> {

  void onMessageEnd();
  void close() throws IOException;
}
