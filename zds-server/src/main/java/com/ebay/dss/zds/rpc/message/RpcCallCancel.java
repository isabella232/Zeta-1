package com.ebay.dss.zds.rpc.message;

import com.ebay.dss.zds.rpc.RpcAddress;
import com.ebay.dss.zds.rpc.RpcAddress.RpcAddressSource;

/**
 * Created by tatian on 2020-09-09.
 */
public class RpcCallCancel extends RpcRequestMessage<Long, Boolean> {

  private final RpcAddressSource targetRpcAddressSource;
  public RpcCallCancel(Long id, RpcAddress sender, RpcAddress receiver) {
    super(id, sender, receiver);
    this.targetRpcAddressSource = new RpcAddressSource(id, sender.host, sender.port, sender.name);
  }

  /**
   * Check if the id and sender equals, that this message is the target cancel message
   * Actually the id should be (or guarantee) global unique, so only id match can tells the result
   * **/
  public boolean canRetrieve(RpcMessage other) {
    return this.message.equals(other.id) && this.sender.equals(other.sender);
  }

  public RpcAddressSource getTargetRpcAddressSource() {
    return this.targetRpcAddressSource;
  }
}
