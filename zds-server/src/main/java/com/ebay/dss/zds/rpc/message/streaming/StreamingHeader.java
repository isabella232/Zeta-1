package com.ebay.dss.zds.rpc.message.streaming;

import java.io.Serializable;

/**
 * Created by tatian on 2020-09-13.
 */
public class StreamingHeader implements Serializable {

  // actually the source request id when this is a backward message
  // and the push notification id when it's forward
  public final long streamingTransferId;

  public StreamingHeader(long streamingTransferId) {
    this.streamingTransferId = streamingTransferId;
  }
}
