package com.ebay.dss.zds.rpc;

import com.ebay.dss.zds.rpc.message.RpcAliveDetect;
import com.ebay.dss.zds.rpc.message.RpcCallCancel;
import com.ebay.dss.zds.rpc.message.RpcConnectionEstablish;
import com.ebay.dss.zds.rpc.message.RpcMessage;

import java.util.List;
import java.util.stream.Collectors;

import static com.ebay.dss.zds.rpc.RpcAddress.CONTROLLER;

/**
 * Created by tatian on 2020-09-16.
 */
public class RpcEndpointController extends RpcEndpoint {

  protected RpcEndpointController(RpcEnv rpcEnv) throws Exception {
    super(rpcEnv, CONTROLLER);
  }

  @Override
  public void handleReceive(RpcCallContext callContext) {

    RpcMessage message = callContext.rpcMessage;
    if (message instanceof RpcCallCancel) {
      this.rpcEnv.cancelSync(callContext);
    } else if (message instanceof RpcConnectionEstablish) {
      List<RpcAddress> rpcAddressList = this.rpcEnv
              .getRegisteredRpcEndpointRefs()
              .stream()
              .filter(RpcEndpointRef::isLocal)
              .map(RpcEndpointRef::getRpcAddress)
              .collect(Collectors.toList());
      callContext.reply(new RpcAddress.RpcAddressSet(rpcAddressList));
    } else if (message instanceof RpcAliveDetect) {
      callContext.reply(new RpcAliveDetect.DetectBack());
    }
  }

}
