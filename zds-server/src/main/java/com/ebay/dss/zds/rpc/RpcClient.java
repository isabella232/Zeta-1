package com.ebay.dss.zds.rpc;

import com.ebay.dss.zds.rpc.exception.RpcSendFailException;
import com.ebay.dss.zds.rpc.message.RpcMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created by tatian on 2020-09-06.
 */
public class RpcClient {

  protected static final Logger logger = LoggerFactory.getLogger(RpcClient.class);

  private RpcEnv rpcEnv;
  public final RpcAddress rpcAddress;
  private Channel channel;

  public RpcClient(RpcEnv rpcEnv, RpcAddress rpcAddress) {
    this.rpcEnv = rpcEnv;
    this.rpcAddress = rpcAddress;
  }

  public RpcClient setup() throws InterruptedException {
    this.channel = rpcEnv.createClientChannel(rpcAddress.host, rpcAddress.port);
    return this;
  }

  public Channel getChannel() {
    return this.channel;
  }

  public boolean isActive() {
    return this.channel.isActive();
  }

  public void writeAndFlush(RpcMessage message) throws RpcSendFailException {
    //ByteBuf byteBuf = rpcEnv.toByteBuf(message);
    try {
      channel.writeAndFlush(message).sync();
//      channel.writeAndFlush(message).addListener(new ChannelFutureListener() {
//        @Override
//        public void operationComplete(ChannelFuture cf) {
//          if (!cf.isSuccess()) {
//            logger.warn("Failed when write the message to rpc channel: {}", message.toJson());
//            rpcEnv.handleRpcEndpointCallFailed(message,
//                    new RpcSendFailException("Failed when write the message to rpc channel"));
//          }
//        }
//      });
    } catch (Exception ex) {
      throw new RpcSendFailException(ex);
    }
  }

  public void stop() {
    try {
      this.channel.disconnect().sync();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

}
