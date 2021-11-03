package com.ebay.dss.zds.rpc;

import com.ebay.dss.zds.rpc.exception.RpcMessageIgnoredException;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by tatian on 2020-09-06.
 */
public abstract class RpcEndpoint {

  protected final RpcEndpointRef ref;
  protected final RpcEnv rpcEnv;
  protected final String name;

  public RpcEndpoint(RpcEnv rpcEnv, String name) throws Exception {
    this.rpcEnv = rpcEnv;
    this.name = name;
    this.ref = this.registerSelf();
  }

  private RpcEndpointRef registerSelf() throws Exception {
    return this.rpcEnv.registerRpcEndpointRef(this);
  }

  public RpcAddress getRpcAddress() {
    return this.ref.getRpcAddress();
  }

  // handle by process thread pool in dispatcher
  public void onReceive(RpcCallContext callContext) {
    // if get exception here, the message could be handled by the processor
    handleReceive(callContext);
    // if the message need to reply throw an exception back;
    if (callContext.needReply() && !callContext.replied()) {
      callContext.sendFailure(new RpcMessageIgnoredException("This message: "
              + callContext.rpcMessage.getClass().getName()
              + " was ignored by the receiver: "
              + ref.getRpcAddress().toString()));
    }

    if (callContext.mustCheck() && !callContext.checked()) {
      callContext.rpcMessage.callOutIgnore();
      callContext.rpcMessage.markAsChecked();
    }
  }

  public abstract void handleReceive(RpcCallContext callContext);

  public void doWithRpcEnv(Consumer<RpcEnv> fn) {
    fn.accept(this.rpcEnv);
  }

}
