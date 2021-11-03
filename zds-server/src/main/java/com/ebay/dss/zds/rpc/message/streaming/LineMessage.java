package com.ebay.dss.zds.rpc.message.streaming;

import java.io.Serializable;

/**
 * Created by tatian on 2020-09-26.
 */
public class LineMessage<T extends Serializable> implements Serializable {

  public final T data;

  public LineMessage(T data) {
    this.data = data;
  }
}
