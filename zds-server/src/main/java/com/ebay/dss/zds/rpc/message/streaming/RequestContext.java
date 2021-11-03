package com.ebay.dss.zds.rpc.message.streaming;

import java.io.Serializable;

/**
 * Created by tatian on 2020-09-13.
 */
public class RequestContext<T extends Serializable> implements Serializable {

  // todo: make it final
  private String user;
  private T requestEntity;

  public RequestContext(T requestEntity, String user) {
    this.requestEntity = requestEntity;
    this.user = user;
  }

  public RequestContext() {
  }

  public T getRequestEntity() {
    return this.requestEntity;
  }

  public String getUser() {
    return user;
  }
}
