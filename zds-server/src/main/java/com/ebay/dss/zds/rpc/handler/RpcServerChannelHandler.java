package com.ebay.dss.zds.rpc.handler;

import com.ebay.dss.zds.rpc.RpcEnv;
import com.ebay.dss.zds.rpc.message.RpcMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created by tatian on 2020-09-06.
 */
public class RpcServerChannelHandler extends SimpleChannelInboundHandler<RpcMessage> {

  protected static final Logger logger = LoggerFactory.getLogger(RpcServerChannelHandler.class);

  private RpcEnv rpcEnv;

  public RpcServerChannelHandler(RpcEnv rpcEnv) {
    this.rpcEnv = rpcEnv;
    logger.info("RpcServerChannelHandler created");
  }

  @Override
  public void channelRead0(ChannelHandlerContext ctx, RpcMessage msg) throws Exception {
    msg.setInBoundTime(System.currentTimeMillis());
    rpcEnv.tell(msg);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    super.exceptionCaught(ctx, cause);
    cause.printStackTrace();
  }

}
