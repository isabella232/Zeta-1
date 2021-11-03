package com.ebay.dss.zds.rpc.message;

import com.ebay.dss.zds.rpc.RpcAddress;

/**
 * Created by tatian on 2020-09-07.
 */
public class RpcConnectionEstablish extends RpcRequestMessage<String, RpcAddress.RpcAddressSet> {

  public RpcConnectionEstablish(RpcAddress sender, RpcAddress receiver) {
    super("establish", sender, receiver);
  }
}
