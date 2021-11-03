package com.ebay.dss.zds.rpc.message.streaming;

import java.io.Serializable;

/**
 * Created by tatian on 2020-09-13.
 */
public class StreamingEnd implements Serializable {

  public final long streamingTransferId;
  public final long currentBlockId;
  public final long totalBlocks;

  public StreamingEnd(long streamingTransferId, long currentBlockId, long totalBlocks) {
    this.streamingTransferId = streamingTransferId;
    this.currentBlockId = currentBlockId;
    this.totalBlocks = totalBlocks;
  }
}
