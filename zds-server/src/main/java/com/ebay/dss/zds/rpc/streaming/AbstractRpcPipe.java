package com.ebay.dss.zds.rpc.streaming;

import com.ebay.dss.zds.rpc.AbstractRpcStreamingManager;
import com.ebay.dss.zds.rpc.RpcStreamingManager;
import com.ebay.dss.zds.rpc.message.streaming.StreamingBlock;
import com.ebay.dss.zds.rpc.message.streaming.StreamingEnd;

import java.io.IOException;
import java.io.Serializable;
import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by tatian on 2020-09-13.
 */

/**
 * Actually this object won't be serialized and transferred but generated in
 * target side by the rpc request context
 **/
public abstract class AbstractRpcPipe<T extends Serializable> implements Serializable {

  private final long pipeId;
  private PipeManager pipeManager;
  private volatile long currentBlockId;
  private volatile long nextBlockId;
  private volatile long endBlockId = -1;
  private AtomicLong receivedBlockNums = new AtomicLong(0);
  private AtomicLong insertedBlockNums = new AtomicLong(0);
  private volatile boolean closeWhenTransferEnd = false;
  // we give this a very small capacity because we expected the out of sequence case could nearly never happen
  private PriorityBlockingQueue<StreamingBlock<T>> waits = new PriorityBlockingQueue<>(1000, comparator);

  private volatile long streamingEndTime = -1;

  // order by block id, the smallest is in the head of queue
  public static final Comparator<StreamingBlock> comparator =
          (StreamingBlock l, StreamingBlock r) -> {
            long sub = l.currentBlockId - r.currentBlockId;
            return sub > 0 ? 1 : (sub < 0 ? -1 : 0);
          };

  public AbstractRpcPipe(long pipeId, PipeManager pipeManager, long nextBlockId) {
    this.pipeId = pipeId;
    this.pipeManager = pipeManager;
    this.nextBlockId = nextBlockId;
  }

  public synchronized void onNewBlock(StreamingBlock<T> block) {
    try {
      if (block.currentBlockId == nextBlockId) {
        insertNewBlock(block);
        handleWaitingBlocks();
      } else {
        this.waits.offer(block);
      }
    } finally {
      receivedBlockNums.incrementAndGet();
    }
  }

  // This method handle the block by the order restrict

  /**
   * so when  block.currentBlockId == endBlockId
   **/
  // it means the blocks before this block are all inserted
  private void insertNewBlock(StreamingBlock<T> block) {
    this.currentBlockId = block.currentBlockId;
    this.nextBlockId = block.nextBlockId;
    innerConsume(block.data);
    this.insertedBlockNums.incrementAndGet();
    /**When the last block inserted marked has no input**/
    if (block.currentBlockId == endBlockId) {
      markedNoInput();
    }
  }

  private void handleWaitingBlocks() {
    if (this.waits.size() > 0) {
      StreamingBlock block = this.waits.peek();
      while (block != null && block.currentBlockId == this.nextBlockId) {
        // remove it
        this.waits.poll();
        insertNewBlock(block);
        block = this.waits.peek();
      }
    }
  }

  public boolean receiveCompleted() {
    return endBlockId == this.receivedBlockNums.get();
  }

  public synchronized void onStreamingEnd(StreamingEnd streamingEnd) {
    this.endBlockId = streamingEnd.totalBlocks;
    if (insertedBlockNums.get() == this.endBlockId) {
      markedNoInput();
    }
  }

  private void markedNoInput() {
    innerStreamEnd();
    this.streamingEndTime = System.currentTimeMillis();
    if (closeWhenTransferEnd) {
      try {
        close();
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }

  public void close() throws IOException {
    this.pipeManager.removePipe(this.pipeId);
    innerClose();
  }

  protected abstract void innerConsume(T data);
  protected abstract void innerStreamEnd();
  protected abstract void innerClose() throws IOException;

  public long getCurrentBlockId() {
    return currentBlockId;
  }

  public long getNextBlockId() {
    return nextBlockId;
  }

  // this will marked pipe as ended
  protected void setEndBlockId(long endBlockId) {
    this.endBlockId = endBlockId;
  }

  public int waitsCnt() {
    return this.waits.size();
  }

  public long streamingEndTime() {
    return this.streamingEndTime;
  }

  public void closeWhenTransferEnd() {
    this.closeWhenTransferEnd = true;
  }
}
