package com.ebay.dss.zds.rpc.message.streaming;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by tatian on 2020-09-13.
 */
public class StreamingBlock<T extends Serializable> implements Serializable {

  public final long streamingTransferId;
  // The block id should start from 1 per streaming pipe
  public final long currentBlockId;
  public final long nextBlockId;
  public final T data;

  private StreamingBlock(long streamingTransferId, long currentBlockId, long nextBlockId, T data) {
    this.streamingTransferId = streamingTransferId;
    this.currentBlockId = currentBlockId;
    this.nextBlockId = nextBlockId;
    this.data = data;
  }

  public static final long firstBlockId = 1L;

  public static class BlockBuilder<T extends Serializable> {
    private final AtomicLong index = new AtomicLong(firstBlockId);
    private long streamingTransferId;

    public BlockBuilder(long streamingTransferId) {
      this.streamingTransferId = streamingTransferId;
    }

    public StreamingHeader streamingHeader() {
      return new StreamingHeader(streamingTransferId);
    }

    public StreamingBlock newBlock(T data) {
      long current = this.index.getAndIncrement();
      return new StreamingBlock<>(streamingTransferId, current, this.index.get(), data);
    }

    public StreamingEnd streamingEnd() {
      long generatedBlocks = generatedBlocksCnt();
      long current = this.index.getAndIncrement();
      this.index.set(firstBlockId);
      return new StreamingEnd(streamingTransferId, current, generatedBlocks);
    }

    public long getStreamingTransferId() {
      return streamingTransferId;
    }

    public void setStreamingTransferId(long streamingTransferId) {
      this.streamingTransferId = streamingTransferId;
    }

    public long currentIndex() {
      return this.index.get();
    }

    public long lastBlockIndex() {
      return this.index.get() - 1;
    }

    public long generatedBlocksCnt() {
      return this.index.get() - 1;
    }
  }

}
