package com.ebay.dss.zds.cluster;

import com.ebay.dss.zds.common.QueueDestination;
import com.ebay.dss.zds.rpc.message.streaming.LineMessage;
import com.ebay.dss.zds.rpc.streaming.RemoteRpcStreamingPipe;
import com.ebay.dss.zds.websocket.WebSocketResp;

/**
 * Created by tatian on 2021-02-03.
 */
public class RemoteQueueDestination extends QueueDestination {

  private RemoteRpcStreamingPipe pipe;

  public RemoteQueueDestination(String userName, String queue, RemoteRpcStreamingPipe pipe) {
    super(userName, null, queue);
    this.pipe = pipe;
  }

  @Override
  public <T> void sendData(WebSocketResp<T> payload){
    pipe.writeAndFlush(new LineMessage(new WebSocketRespWrapper(getUserName(), getQueue(), payload)));
  }

}
