package com.ebay.dss.zds.rpc.streaming;

/**
 * Created by tatian on 2020-09-26.
 */
public interface PipeManager<T extends AbstractRpcPipe> {

  T registerPipe(long pipeId, T pipe);

  T removePipe(long pipeId);
}
