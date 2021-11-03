package com.ebay.dss.zds.rpc.streaming;

import com.ebay.dss.zds.rpc.RpcAddress;
import com.ebay.dss.zds.rpc.RpcCallContext;
import com.ebay.dss.zds.rpc.RpcEnv;
import com.ebay.dss.zds.rpc.annotation.ConcurrencyNotAllowed;
import com.ebay.dss.zds.rpc.message.RpcMessage;
import com.ebay.dss.zds.rpc.message.streaming.*;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Created by tatian on 2020-09-14.
 *
 * Don't do write operation on this pipe in different thread
 */
@ConcurrencyNotAllowed
public class RemoteRpcStreamingPipe<T extends Serializable, V extends Class<T>> implements AutoCloseable {

  private RpcEnv rpcEnv;
  private RpcCallContext callContext;
  private long streamingId;
  private RpcAddress sender;
  private RpcAddress receiver;
  private volatile boolean started = false;
  private volatile boolean end = false;
  private StreamingBlock.BlockBuilder blockBuilder;
  private LinkedBlockingQueue<RpcMessage> buffer;
  private AtomicLong bufferedNum = new AtomicLong();
  private AtomicLong bufferedSize = new AtomicLong();
  private int maxBufferedMessageNum;
  private long maxBufferedMessageSize;

  private long firstBlockId;
  private boolean forward;
  private Class messageType;

  public RemoteRpcStreamingPipe(RpcCallContext callContext, RpcStreamingFetch rpcMessage) {
    this.rpcEnv = callContext.rpcEnv;
    this.streamingId = rpcMessage.id;
    this.blockBuilder = new StreamingBlock.BlockBuilder(streamingId);
    this.buffer = new LinkedBlockingQueue<>();
    this.forward = false;
    this.sender = rpcMessage.getRealTarget();
    this.receiver = rpcMessage.getSender();
    this.callContext = callContext;
    this.messageType = byte[].class;
    setupPipeContext();
  }

  public RemoteRpcStreamingPipe(RpcCallContext callContext, RpcMessageChannelFetch rpcMessage) {
    this.rpcEnv = callContext.rpcEnv;
    this.streamingId = rpcMessage.id;
    this.blockBuilder = new StreamingBlock.BlockBuilder(streamingId);
    this.buffer = new LinkedBlockingQueue<>();
    this.forward = false;
    this.sender = rpcMessage.getRealTarget();
    this.receiver = rpcMessage.getSender();
    this.callContext = callContext;
    this.messageType = LineMessage.class;
    setupPipeContext();
  }

  public RemoteRpcStreamingPipe(RpcEnv rpcEnv, RpcAddress sender, RpcAddress receiver, V messageType) {
    this.rpcEnv = rpcEnv;
    this.sender = sender;
    this.receiver = receiver;
    this.callContext = new RpcCallContext(new RpcStreamingPushNotify(new PushContext(), sender, receiver), rpcEnv);
    this.streamingId = callContext.rpcMessage.id;
    this.blockBuilder = new StreamingBlock.BlockBuilder(streamingId);
    this.buffer = new LinkedBlockingQueue<>();
    this.forward = true;
    this.messageType = messageType;
    setupPipeContext();
  }

  private void setupPipeContext() {
    this.firstBlockId = this.blockBuilder.currentIndex();
    this.maxBufferedMessageNum = this.rpcEnv.getRpcConf().getRpcStreamingPipeRemoteBufferNum();
    this.maxBufferedMessageSize = this.rpcEnv.getRpcConf().getRpcStreamingPipeRemoteBufferSize();
  }

  private void insertIntoBuffer(RpcMessage message) {
    this.buffer.offer(message);
    this.bufferedNum.incrementAndGet();
  }

  private boolean reachLimit() {
    return this.bufferedNum.get() >= maxBufferedMessageNum || this.bufferedSize.get() >= maxBufferedMessageSize;
  }

  private void checkIfCanFlush() {
    if (reachLimit()) this.flush();
  }

  public void flush() {
    RpcMessage message = buffer.poll();
    while (message != null) {
      callContext.push(message);
      message = buffer.poll();
    }
    this.bufferedNum.set(0);
    this.bufferedSize.set(0);
  }

  public void start() {
    if (!this.started && !this.end) {
      insertIntoBuffer(header());
      this.started = true;
    }
  }

  public void write(T data) {
    assert data.getClass().equals(messageType);
    if (!this.started) start();
    if (!this.end) {
      insertIntoBuffer(block(data));
      countSize(data);
      checkIfCanFlush();
    }
  }

  public void writeAndFlush(T data) {
    write(data);
    flush();
  }

  public void end() {
    if (!this.end && this.started) {
      insertIntoBuffer(streamingEnd());
      this.end = true;
    }
    flush();
  }

  public void inPipeRun(Consumer<RemoteRpcStreamingPipe> runnable) {
    this.start();
    runnable.accept(this);
    this.end();
  }

  private RpcStreamingHeader header() {
    return new RpcStreamingHeader(messageType, streamingId, firstBlockId,
            sender, receiver, forward);
  }

  private RpcStreamingBlock block(T data) {
    return new RpcStreamingBlock(messageType, streamingId, this.blockBuilder.newBlock(data),
            sender, receiver, forward);
  }

  private RpcStreamingEnd streamingEnd() {
    return new RpcStreamingEnd(messageType, streamingId, this.blockBuilder.streamingEnd(),
            sender, receiver, forward);
  }

  private void countSize(T data) {
    if (data instanceof byte[]) {
      this.bufferedSize.addAndGet(((byte[])data).length);
    }
  }

  @Override
  public void close() {
    this.end();
  }

}
