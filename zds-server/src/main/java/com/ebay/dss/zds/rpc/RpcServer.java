package com.ebay.dss.zds.rpc;

import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by tatian on 2020-09-06.
 */
public class RpcServer {

  protected static final Logger logger = LoggerFactory.getLogger(RpcServer.class);

  private RpcEnv rpcEnv;
  public final int port;
  private Channel channel;
  //private Thread channelKeepThread;

  public RpcServer(RpcEnv rpcEnv, int port) {
    this.rpcEnv = rpcEnv;
    this.port = port;
  }

  public RpcServer setup() throws InterruptedException {
    this.channel = rpcEnv.createServerChannel(port);
    logger.info("RpcServer: {} setup", port);
    return this;
  }

  public Channel getChannel() {
    return this.channel;
  }

  public boolean isActive() {
    return this.channel.isActive();
  }

  public void stop() {
    this.channel.close();
  }

}
