package com.ebay.dss.zds.transport;

/**
 * Created by tatian on 2019-10-30.
 */
public interface TransportProcess<T, V, K> {

  public void beforeTransport(T t);

  public void onTransport(V v);

  public void afterTransport(K k);
}
