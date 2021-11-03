package com.ebay.dss.zds.rpc;

import com.ebay.dss.zds.rpc.exception.RpcEndpointStoppedException;
import com.ebay.dss.zds.rpc.message.RpcReplyMessage;
import com.ebay.dss.zds.rpc.message.RpcRequestMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by tatian on 2020-09-06.
 */
public class RpcEndpointRef {

  protected static final Logger logger = LoggerFactory.getLogger(RpcEndpointRef.class);

  private RpcEnv rpcEnv;
  public final RpcAddress rpcAddress;
  private RpcClient rpcClient;
  private volatile RpcEndpoint localEndpoint;
  private volatile boolean isLocal;
  private volatile boolean started;

  private LinkedBlockingDeque<RpcCallContext> inbox = new LinkedBlockingDeque<>();
  protected LinkedBlockingDeque<RpcCallContext> unRepliedBox = new LinkedBlockingDeque<>();

  public RpcEndpointRef(RpcEnv rpcEnv, RpcAddress rpcAddress, RpcEndpoint localEndpoint) throws Exception {
    this.rpcEnv = rpcEnv;
    this.rpcAddress = rpcAddress;
    if (localEndpoint != null) {
      this.localEndpoint = localEndpoint;
      this.isLocal = true;
    } else {
      if (rpcAddress == null) throw new IllegalArgumentException("Please provide RpcAddress");
      this.rpcClient = rpcEnv.createRpcClient(rpcAddress.host, rpcAddress.port, rpcAddress.name);
      this.isLocal = false;
    }
    this.started = true;
  }

  public RpcEndpointRef(RpcEnv rpcEnv, RpcEndpoint localEndpoint) throws Exception {
    this(rpcEnv, new RpcAddress(rpcEnv.host, rpcEnv.port, localEndpoint.name), localEndpoint);
  }

  public RpcEndpointRef(RpcEnv rpcEnv, RpcAddress rpcAddress) throws Exception {
    this(rpcEnv, rpcAddress, null);
  }

  public void inboxOffer(RpcCallContext callContext) {
    if (!started)
      throw new IllegalStateException("The RpcEndpointRef: " + this.rpcAddress.toString() + " already stopped");
    this.inbox.offer(callContext);
  }

  public RpcCallContext inboxTake() throws Exception {
    if (!started)
      throw new IllegalStateException("The RpcEndpointRef: " + this.rpcAddress.toString() + " already stopped");
    return this.inbox.take();
  }

  public boolean handleReplyMessage(RpcReplyMessage rpcReplyMessage) {
    // a inbound reply message, we set the value directly to the unReplied rpc request
    // and don't need to call onReceive
    Iterator<RpcCallContext> it = unRepliedBox.iterator();
    boolean replied = false;
    while (it.hasNext()) {
      RpcCallContext unReplied = it.next();
      if (unReplied.rpcMessage.id == rpcReplyMessage.sourceId
              && unReplied.rpcMessage instanceof RpcRequestMessage) {
        it.remove();
        RpcRequestMessage rpcRequestMessage = (RpcRequestMessage) unReplied.rpcMessage;
        if (rpcReplyMessage.isSuccess()) {
          rpcRequestMessage.setResult(rpcReplyMessage.getMessage());
        } else {
          rpcRequestMessage.setFailure(rpcReplyMessage.getThrowable());
        }
        replied = true;
      }
    }
    return replied;
  }

  // todo: handle the error
  public void onReceive(RpcCallContext msg) throws Exception {
    if (!started) {
      throw new IllegalStateException("The RpcEndpointRef: " + this.rpcAddress.toString() + " already stopped");
    }
    if (isLocal) {
      this.localEndpoint.onReceive(msg);
    } else {
      rpcClient.writeAndFlush(msg.rpcMessage);
    }
  }

  public boolean isLocal() {
    return this.isLocal;
  }

  public boolean isStarted() {
    return started;
  }

  public RpcEnv getRpcEnv() {
    return rpcEnv;
  }

  public RpcAddress getRpcAddress() {
    return rpcAddress;
  }
  public RpcAddress getControllerRpcAddress() {
    return this.getRpcAddress().getControllerRpcAddress();
  }

  public void destroy(boolean immediate) {
    this.started = false;
    if (immediate) {
      rpcClient.stop();
      this.inbox.clear();
      this.unRepliedBox.clear();
    } else {
      cleanUpUnReplied();
      cleanUpInbox();
    }
  }

  private void cleanUpInbox() {
    Iterator<RpcCallContext> iterator = this.inbox.iterator();
    while (iterator.hasNext()) {
      RpcCallContext callContext = iterator.next();
      rpcEnv.handleRpcEndpointStopped(callContext.rpcMessage);
    }
    this.inbox.clear();
  }

  private void cleanUpUnReplied() {
    Iterator<RpcCallContext> iterator = this.unRepliedBox.iterator();
    while (iterator.hasNext()) {
      RpcCallContext callContext = iterator.next();
      RpcRequestMessage rpcRequestMessage = (RpcRequestMessage) callContext.rpcMessage;
      rpcRequestMessage.setFailure(new RpcEndpointStoppedException(rpcRequestMessage.getReceiver(), "No any endpoint found"));
    }
    this.unRepliedBox.clear();
  }

  protected LinkedBlockingDeque<RpcCallContext> getInbox() {
    return this.inbox;
  }

  @Nullable
  protected RpcEndpoint getLocalRpcEndpoint() {
    return localEndpoint;
  }

}
