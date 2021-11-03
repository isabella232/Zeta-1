package com.ebay.dss.zds.rpc.message;

import com.ebay.dss.zds.rpc.RpcAddress;

/**
 * Created by tatian on 2020-09-10.
 */
public class RpcEndpointsFetch extends RpcConnectionEstablish {

  public RpcEndpointsFetch(RpcAddress sender, RpcAddress receiver) {
    super(sender, receiver);
  }
}
