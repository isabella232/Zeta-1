package com.ebay.dss.zds.interpreter.interpreters.livy.k8s;

/**
 * Created by tatian on 2020-11-20.
 */
public class K8sEndpoint {

  public final String hostname;
  public final String ip;

  public K8sEndpoint(String hostname, String ip) {
    this.hostname = hostname;
    this.ip = ip;
  }
}
