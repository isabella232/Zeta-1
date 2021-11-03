package com.ebay.dss.zds.rpc.message.streaming;

/**
 * Created by tatian on 2021-02-03.
 */
public class LineMessageEnd extends LineMessage<byte[]> {

  public static final LineMessageEnd LINE_END = new LineMessageEnd();

  public LineMessageEnd() {
    super("LINE_END".getBytes());
  }

}
