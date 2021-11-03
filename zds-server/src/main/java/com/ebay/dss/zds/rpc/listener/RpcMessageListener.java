package com.ebay.dss.zds.rpc.listener;

import java.io.Serializable;

/**
 * Created by tatian on 2020-09-24.
 */
public interface RpcMessageListener<T extends Serializable> {

  void onIgnore();
  void onMessage(T message);

}
