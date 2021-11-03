package com.ebay.dss.zds.rpc;

import com.ebay.dss.zds.rpc.exception.RpcEndpointNotFoundException;
import com.ebay.dss.zds.rpc.exception.UnexpectedRpcMessageException;
import com.ebay.dss.zds.rpc.listener.RpcMessageListener;
import com.ebay.dss.zds.rpc.message.RpcMessage;
import com.ebay.dss.zds.rpc.message.RpcReplyMessage;
import com.ebay.dss.zds.rpc.message.streaming.*;
import com.ebay.dss.zds.rpc.streaming.AbstractRpcPipe;
import com.ebay.dss.zds.rpc.streaming.PipeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by tatian on 2020-09-13.
 */
public abstract class AbstractRpcStreamingManager<V extends Serializable, T extends AbstractRpcPipe<V>>
        extends RpcEndpoint implements PipeManager<T> {

  private static final Logger logger = LoggerFactory.getLogger(AbstractRpcStreamingManager.class);

  private ConcurrentHashMap<Long, T> streamingPipes = new ConcurrentHashMap<>();
  private final ReentrantLock lock = new ReentrantLock();

  protected AbstractRpcStreamingManager(RpcEnv rpcEnv, String name) throws Exception {
    super(rpcEnv, name);
  }

  // the rpcCallContext must be streaming message
  public void handleReceive(RpcCallContext callContext) {
    if (callContext.streamingMessage()) {

      RpcMessage message = callContext.rpcMessage;
      RpcAddress realReceiver = ((RpcStreamingMessage) message).getRealTarget();

      if (message instanceof RpcStreamingBlock) {
        onRpcStreamingBlock((RpcStreamingBlock) message);
      } else if (message instanceof RpcStreamingRequestMessage) {
        onRpcStreamingRequestMessage(callContext, (RpcStreamingRequestMessage) message, realReceiver);
      } else if (message instanceof RpcStreamingHeader) {
        onRpcStreamingHeader((RpcStreamingHeader) message, realReceiver);
      } else if (message instanceof RpcStreamingEnd) {
        onRpcStreamingEnd((RpcStreamingEnd) message);
      }
      // the message is ignored, handle the ignored case out side

    } else {
      callContext.sendFailure(new UnexpectedRpcMessageException("This is not a rpc streaming message"));
      logger.error("This is not a rpc streaming message: {}", callContext.rpcMessage.toJson());
    }
  }

  private void onRpcStreamingBlock(RpcStreamingBlock streamingBlock) {
    StreamingBlock<V> block = streamingBlock.getMessage();
    long streamingId = block.streamingTransferId;
    T pipe;
    if (streamingPipes.containsKey(streamingId)) {
      pipe = this.streamingPipes.get(streamingId);
      pipe.onNewBlock(block);
    } else {
      try {
        lock.lock();
        // This might be message
        /** OUT OF SEQUENCE  **/
        if (!streamingPipes.containsKey(streamingId)) {
          pipe = createNewPipe(streamingBlock.messageType, streamingId, StreamingBlock.firstBlockId);
          registerStreamingPipe(streamingId, pipe);
        } else {
          pipe = streamingPipes.get(streamingId);
        }
      } finally {
        lock.unlock();
      }
      pipe.onNewBlock(block);
    }
  }

  private void onRpcStreamingRequestMessage(RpcCallContext callContext, RpcStreamingRequestMessage requestMessage, RpcAddress realReceiver) {
    RpcEndpointRef ref = rpcEnv.getRegisteredRpcEndpointRef(realReceiver);
    if (ref == null || !ref.isLocal()) {
      callContext.sendFailure(new RpcEndpointNotFoundException(realReceiver,
              "No rpc endpoint found by streaming manager in local"));
      return;
    }
    requestMessage.redirectToNewReceiver(realReceiver);
    ref.getLocalRpcEndpoint().handleReceive(callContext);
  }

  private void onRpcStreamingHeader(RpcStreamingHeader streamingHeader, RpcAddress realReceiver) {
    long streamingId = streamingHeader.getMessage().streamingTransferId;
    T pipe;
    if (!streamingPipes.containsKey(streamingId)) {
      try {
        lock.lock();
        if (!streamingPipes.containsKey(streamingId)) {
          pipe = createNewPipe(streamingHeader.messageType, streamingId, StreamingBlock.firstBlockId);
          registerStreamingPipe(streamingId, pipe);
        } else {
          pipe = streamingPipes.get(streamingId);
        }
      } finally {
        lock.unlock();
      }
    } else {
      pipe = streamingPipes.get(streamingId);
    }
    /** inform the target endpoint **/
    RpcMessage forwardMessage;
    if (streamingHeader.forward) {
      forwardMessage = informMessage(pipe, streamingHeader.getSender(), realReceiver);
      forwardMessage.setListener(new RpcMessageListener<Serializable>() {

        @Override
        public void onMessage(Serializable any) {}

        @Override
        public void onIgnore() {
          pipe.closeWhenTransferEnd();
          logger.warn("This RpcStreamingPushNotify has been ignored, close it when transfer end, {}", forwardMessage.toJson());
        }
      });
    } else {
      forwardMessage = new RpcReplyMessage<>(streamingId, pipe, streamingHeader.getSender(), realReceiver);
    }
    /** this is not a rpc request message, must tell it out to the target receiver**/
    rpcEnv.tell(forwardMessage);
  }

  private void onRpcStreamingEnd(RpcStreamingEnd rpcStreamingEnd) {
    StreamingEnd streamingEnd = rpcStreamingEnd.getMessage();
    long streamingId = streamingEnd.streamingTransferId;
    T pipe;
    if (streamingPipes.containsKey(streamingId)) {
      pipe = this.streamingPipes.get(streamingId);
      // the block id is increased by +1 from 1
    } else {
      try {
        lock.lock();
        // This might be message
        /** OUT OF SEQUENCE  **/
        if (!streamingPipes.containsKey(streamingId)) {
          pipe = createNewPipe(rpcStreamingEnd.messageType, streamingId, StreamingBlock.firstBlockId);
          registerStreamingPipe(streamingId, pipe);
        } else {
          pipe = this.streamingPipes.get(streamingId);
        }
      } finally {
        lock.unlock();
      }
    }
    // let the pipe end itself, the block will be process by the order of blockId
    // so when the last blockId processed the pipe will be marked as has no input
    pipe.onStreamingEnd(streamingEnd);
  }

  protected abstract T createNewPipe(Class messageType, long streamingId, long nextBlockId);

  protected abstract RpcMessage informMessage(T pipe, RpcAddress sender, RpcAddress receiver);

  public T registerStreamingPipe(long streamingId, T pipe) {
    return registerPipe(streamingId, pipe);
  }

  public T removeStreamingPipe(long streamingId) {
    return removePipe(streamingId);
  }

  @Override
  public T registerPipe(long streamingId, T pipe) {
    return streamingPipes.put(streamingId, pipe);
  }

  @Override
  public T removePipe(long streamingId) {
    return streamingPipes.remove(streamingId);
  }

}
