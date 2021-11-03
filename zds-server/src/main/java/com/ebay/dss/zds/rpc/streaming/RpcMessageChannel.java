package com.ebay.dss.zds.rpc.streaming;

import com.ebay.dss.zds.rpc.listener.RpcMessageListener;
import com.ebay.dss.zds.rpc.listener.StoppableMessageListener;
import com.ebay.dss.zds.rpc.message.streaming.LineMessage;
import com.ebay.dss.zds.rpc.message.streaming.LineMessageEnd;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

/**
 * Created by tatian on 2020-09-26.
 */
public class RpcMessageChannel extends AbstractRpcPipe<LineMessage> {

  private LinkedBlockingQueue<LineMessage> messageChannel;

  public RpcMessageChannel(long streamingId,
                           PipeManager pipeManager,
                           long nextBlockId) {
    super(streamingId, pipeManager, nextBlockId);
    this.messageChannel = new LinkedBlockingQueue<>();
  }

  @Override
  protected void innerConsume(LineMessage data) {
    this.messageChannel.offer(data);
  }

  @Override
  protected void innerStreamEnd() {
    this.messageChannel.offer(LineMessageEnd.LINE_END);
  }

  @Override
  protected void innerClose() throws IOException {
    this.messageChannel.clear();
  }

  public void processLineMessage(Consumer<LineMessage> messageHandler) throws Exception {
    LineMessage message = this.messageChannel.take();
    while (!(message instanceof LineMessageEnd)) {
      messageHandler.accept(message);
      message = this.messageChannel.take();
    }
  }

  public void processLineMessage(RpcMessageListener<LineMessage> listener) throws Exception {
    processLineMessage(listener::onMessage);
  }

  public void processLineMessage(StoppableMessageListener<LineMessage> listener) throws Exception {
    LineMessage message = this.messageChannel.take();
    while (!(message instanceof LineMessageEnd) && !listener.canStop()) {
      listener.onMessage(message);
      message = this.messageChannel.take();
    }
  }
}
