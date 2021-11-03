package com.ebay.dss.zds.rpc;

import com.ebay.dss.zds.rpc.message.*;
import com.ebay.dss.zds.rpc.message.streaming.*;
import com.ebay.dss.zds.rpc.streaming.RemoteRpcStreamingPipe;

import java.io.Serializable;

/**
 * Created by tatian on 2020-09-06.
 */
public class RpcCallContext {

  public final RpcMessage rpcMessage;
  public final RpcEnv rpcEnv;
  private volatile boolean replied = false;

  public RpcCallContext(RpcMessage rpcMessage, RpcEnv rpcEnv) {
    this.rpcMessage = rpcMessage;
    this.rpcEnv = rpcEnv;
  }

  public boolean replied() {
    return this.replied;
  }

  public boolean needReply() {
    return rpcMessage instanceof RpcRequestMessage;
  }

  public boolean mustCheck() {
    return this.rpcMessage.isMustCheck();
  }

  public boolean checked() {
    return this.rpcMessage.checked();
  }

  public void markAsChecked() {
    this.rpcMessage.markAsChecked();
  }

  public void push(RpcMessage message) {
    this.rpcEnv.tell(message);
    this.replied = true;
  }


  public void reply(Serializable object) {
    if (object instanceof RpcMessage) {
      rpcEnv.tell(transToReply((RpcMessage) object));
    } else {
      RpcMessage message;
      if (rpcMessage instanceof RpcRequestMessage) {
        RpcRequestMessage request = (RpcRequestMessage) rpcMessage;
        message = new RpcCallSuccess(request.id, object, rpcMessage.getReceiver(), rpcMessage.getSender());
      } else {
        message = new RpcOneWayMessage(object, rpcMessage.getReceiver(), rpcMessage.getSender());
      }
      rpcEnv.tell(message);
    }
    this.replied = true;
  }

  private RpcMessage transToReply(RpcMessage original) {
    RpcMessage message;
    if (original instanceof RpcReplyMessage) {
      message = original;
    } else {
      if (rpcMessage instanceof RpcRequestMessage) {
        RpcRequestMessage request = (RpcRequestMessage) rpcMessage;
        message = new RpcCallSuccess(request.id, original.getMessage(), rpcMessage.getReceiver(), rpcMessage.getSender());
      } else {
        message = new RpcOneWayMessage(original.getMessage(), rpcMessage.getReceiver(), rpcMessage.getSender());
      }
    }
    return message;
  }

  /**
   * Report a failure to the sender.
   */
  public void sendFailure(Throwable e) {
    RpcCallFailed failed = new RpcCallFailed(rpcMessage.id, e, rpcMessage.getReceiver(), rpcMessage.getSender());
    rpcEnv.tell(failed);
    this.replied = true;
  }

  /**
   * The sender of this message.
   */
  public RpcAddress senderAddress() {
    return this.rpcMessage.getSender();
  }

  public RpcAddress receiverAddress() {
    return this.rpcMessage.getReceiver();
  }

  public boolean streamingMessage() {
    return this.rpcMessage instanceof RpcStreamingMessage;
  }

  public RemoteRpcStreamingPipe getBackRemoteRpcStreamingPipe() {
    if (this.rpcMessage instanceof RpcStreamingFetch) {
      // backward
      return new RemoteRpcStreamingPipe<>(this, (RpcStreamingFetch) rpcMessage);
    } else if (this.rpcMessage instanceof RpcMessageChannelFetch) {
      return new RemoteRpcStreamingPipe<>(this, (RpcMessageChannelFetch) this.rpcMessage);
    } else {
      throw new RuntimeException("Can't not establish pipe on this message: " + this.rpcMessage.getClass());
    }
  }
}
