package com.ebay.dss.zds.rpc.streaming;

import com.ebay.dss.zds.rpc.RpcStreamingManager;

import java.io.IOException;

/**
 * Created by tatian on 2020-09-13.
 */

/**
 * Actually this object won't be serialized and transferred but generated in
 * target side by the rpc request context
 **/
public class RpcStreamingPipe extends AbstractRpcPipe<byte[]> {

  private ReadOnceByteArrayInputStream inputStream;

  public RpcStreamingPipe(long streamingId, RpcStreamingManager streamingManager, long nextBlockId, int pollTimeout, boolean tryBest) {
    super(streamingId, streamingManager, nextBlockId);
    this.inputStream = new ReadOnceByteArrayInputStream(pollTimeout, tryBest);
  }

  public RpcStreamingPipe(long streamingId, RpcStreamingManager streamingManager, long nextBlockId, int pollTimeout) {
    super(streamingId, streamingManager, nextBlockId);
    this.inputStream = new ReadOnceByteArrayInputStream(pollTimeout);
  }

  public RpcStreamingPipe(long streamingId, RpcStreamingManager streamingManager, long nextBlockId, boolean tryBest) {
    super(streamingId, streamingManager, nextBlockId);
    this.inputStream = new ReadOnceByteArrayInputStream(tryBest);
  }

  public RpcStreamingPipe(long streamingId, RpcStreamingManager streamingManager, long nextBlockId) {
    super(streamingId, streamingManager, nextBlockId);
    this.inputStream = new ReadOnceByteArrayInputStream();
  }

  public ReadOnceByteArrayInputStream getInputStream() {
    return this.inputStream;
  }

  @Override
  protected void innerConsume(byte[] data) {
    this.inputStream.insert(data);
  }

  @Override
  protected void innerStreamEnd() {
    this.inputStream.markedNoInput();
  }

  @Override
  protected void innerClose() throws IOException {
    this.inputStream.close();
  }

}
