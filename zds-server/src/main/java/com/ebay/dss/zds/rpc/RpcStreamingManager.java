package com.ebay.dss.zds.rpc;

import com.ebay.dss.zds.rpc.message.RpcMessage;
import com.ebay.dss.zds.rpc.message.streaming.*;
import com.ebay.dss.zds.rpc.streaming.AbstractRpcPipe;
import com.ebay.dss.zds.rpc.streaming.RpcMessageChannel;
import com.ebay.dss.zds.rpc.streaming.RpcStreamingPipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

import static com.ebay.dss.zds.rpc.RpcAddress.STREAMING_MANAGER;

/**
 * Created by tatian on 2020-09-13.
 */
public class RpcStreamingManager<T extends Serializable> extends AbstractRpcStreamingManager<T, AbstractRpcPipe<T>> {

  private static final Logger logger = LoggerFactory.getLogger(RpcStreamingManager.class);

  protected RpcStreamingManager(RpcEnv rpcEnv) throws Exception {
    super(rpcEnv, STREAMING_MANAGER);
  }

  @Override
  protected AbstractRpcPipe createNewPipe(Class messageType, long pipeId, long nextBlockId) {
    if (messageType == byte[].class){
      return new RpcStreamingPipe(pipeId, this, nextBlockId);
    } else {
      return new RpcMessageChannel(pipeId, this, nextBlockId);
    }
  }

  @Override
  protected RpcMessage informMessage(AbstractRpcPipe pipe, RpcAddress sender, RpcAddress receiver) {
    if (pipe instanceof RpcStreamingPipe) {
      return new RpcStreamingPushNotify(new PushContext((RpcStreamingPipe)pipe), sender, receiver);
    } else if (pipe instanceof RpcMessageChannel) {
      return new RpcLineMessagePushNotify(new LineMessageContext((RpcMessageChannel) pipe), sender, receiver);
    } else {
      throw new IllegalArgumentException("Unknown pipe class: " + pipe.getClass());
    }
  }

}
