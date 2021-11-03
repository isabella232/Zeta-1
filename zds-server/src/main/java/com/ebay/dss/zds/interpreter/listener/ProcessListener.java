package com.ebay.dss.zds.interpreter.listener;

/**
 * Created by tatian on 2019-10-31.
 */
public interface ProcessListener<T, V, K> {

  void beforeProcess(T t);
  void onProcess(V v);
  void afterProcess(K k);

}
