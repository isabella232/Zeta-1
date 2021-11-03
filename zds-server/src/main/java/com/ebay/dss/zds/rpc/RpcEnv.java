package com.ebay.dss.zds.rpc;

import com.ebay.dss.zds.rpc.exception.RpcCallCanceledException;
import com.ebay.dss.zds.rpc.exception.RpcEndpointNotFoundException;
import com.ebay.dss.zds.rpc.exception.RpcEndpointStoppedException;
import com.ebay.dss.zds.rpc.handler.RpcClientChannelHandler;
import com.ebay.dss.zds.rpc.handler.RpcServerChannelHandler;
import com.ebay.dss.zds.rpc.message.*;
import com.ebay.dss.zds.rpc.serializer.RpcMessageCodec;
import com.ebay.dss.zds.rpc.serializer.RpcMessageCodecInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Future;

import static com.ebay.dss.zds.rpc.RpcAddress.CONTROLLER;

/**
 * Created by tatian on 2020-09-06.
 */
public class RpcEnv {

  protected static final Logger logger = LoggerFactory.getLogger(RpcEnv.class);

  protected RpcConf rpcConf;
  private RpcServer rpcServer;
  private RpcDispatcher rpcDispatcher;
  private RpcEndpointController controller;
  private RpcStreamingManager streamingManager;
  private RpcMessageCodecInitializer codecInitializer;
  public final String host;
  public final int port;
  public final RpcAddress serverAddress;

  private volatile boolean started = false;

  public RpcEnv(Properties properties, int port) throws Exception {
    this.host = InetAddress.getLocalHost().getHostAddress();
    this.port = port;
    this.serverAddress = new RpcAddress(host, port, CONTROLLER);
    this.rpcConf = new RpcConf(properties);
    this.codecInitializer = new RpcMessageCodecInitializer(this.rpcConf);
    this.rpcServer = createRpcServer(port);
    this.rpcDispatcher = new RpcDispatcher(this);
  }

  public void start() throws Exception {
    setupAdminEndpoints();
    this.started = true;
  }

  private void setupAdminEndpoints() throws Exception {
    this.controller = new RpcEndpointController(this);
    this.streamingManager = new RpcStreamingManager(this);
  }

  public void tell(RpcMessage msg) {
    this.rpcDispatcher.postMessage(msg);
  }

  public <V extends Serializable> Future<V> ask(RpcRequestMessage<?, V> msg) {
    this.rpcDispatcher.postMessage(msg);
    return msg.get();
  }

  public void cancelSync(RpcCallContext rpcCallCancel) {
    this.rpcDispatcher.tryAbortMessageAndCallOut(rpcCallCancel);
  }

  protected boolean isCanceled(RpcMessage rpcMessage) {
    return this.rpcDispatcher.isCanceled(rpcMessage);
  }

  protected void handleRpcEndpointCallFailed(RpcMessage message, Throwable throwable) {
    rpcDispatcher.postMessage(new RpcCallFailed(
            message.id,
            throwable,
            message.getReceiver(),
            message.getSender()));
  }

  protected void handleRpcCallCanceled(RpcMessage message) {
    handleRpcEndpointCallFailed(message,
            new RpcCallCanceledException(message.getReceiver(), "The rpc call already been canceled"));
  }

  protected void handleRpcEndpointStopped(RpcMessage message) {
    handleRpcEndpointCallFailed(message,
            new RpcEndpointStoppedException(message.getReceiver(), "No any endpoint found"));
  }

  protected void handleRpcEndpointNotFound(RpcMessage message) {
    handleRpcEndpointCallFailed(message,
            new RpcEndpointNotFoundException(message.getReceiver(), "No any endpoint found"));
  }

  protected Channel createServerChannel(int port) throws InterruptedException {
    RpcEnv self = this;
    Channel channel = new ServerBootstrap()
            .group(new NioEventLoopGroup(rpcConf.getRpcServerBossThreadNum()),
                    new NioEventLoopGroup(rpcConf.getRpcServerWorkerThreadNum()))
            .channel(NioServerSocketChannel.class)
            .option(ChannelOption.SO_BACKLOG, rpcConf.getRpcDispatcherDispatchPoolMax())
            .option(ChannelOption.SO_REUSEADDR, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            //.handler(new LoggingHandler(LogLevel.INFO))
            .childHandler(new ChannelInitializer<SocketChannel>() {
              @Override
              public void initChannel(SocketChannel ch) throws Exception {
                codecInitializer
                        .addCodec(ch.pipeline())
                        .addLast(new RpcServerChannelHandler(self));
              }
            })
            .bind(port)
            .sync()
            .channel();
    logger.info("Server channel created, host: {}, port: {}", this.host, port);
    return channel;
  }

  protected Channel createClientChannel(String host, int port) throws InterruptedException {
    RpcEnv self = this;
    Channel channel = new Bootstrap()
            .group(new NioEventLoopGroup(rpcConf.getRpcClientWorkerThreadNum()))
            .channel(NioSocketChannel.class)
            .option(ChannelOption.TCP_NODELAY, true)
            .option(ChannelOption.SO_REUSEADDR, true)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .handler(new ChannelInitializer<SocketChannel>() {
              @Override
              public void initChannel(SocketChannel ch) throws Exception {
                codecInitializer
                        .addCodec(ch.pipeline())
                        .addLast(new RpcClientChannelHandler(self));
              }
            })
            .connect(host, port)
            .sync()
            .channel();
    logger.info("Client channel created, host: {}, port: {}, isOpen: {}", host, port, channel.isOpen());
    return channel;
  }

  public RpcServer createRpcServer(int port) throws Exception {
    return new RpcServer(this, port).setup();
  }

  public RpcClient createRpcClient(String host, int port, String name) throws Exception {
    return new RpcClient(this, new RpcAddress(host, port, name)).setup();
  }

  public RpcAddress localRpcAddress(int port, String name) {
    return new RpcAddress(host, port, name);
  }

  public RpcAddress controllerRpcAddress(String host, int port) {
    return new RpcAddress(host, port, CONTROLLER);
  }

  public RpcEndpointRef getRegisteredRpcEndpointRef(RpcAddress rpcAddress) {
    return this.rpcDispatcher.getRegisteredRpcEndpointRef(rpcAddress);
  }

  public RpcEndpointRef registerRpcEndpointRef(RpcAddress rpcAddress, RpcEndpointRef ref) throws Exception {
    return this.rpcDispatcher.registerRpcEndpointRef(rpcAddress, ref);
  }

  public RpcEndpointRef registerRpcEndpointRef(RpcAddress rpcAddress, RpcEndpoint rpcEndpoint) throws Exception {
    return this.rpcDispatcher.registerRpcEndpointRef(rpcAddress, rpcEndpoint);
  }

  public RpcEndpointRef registerRpcEndpointRef(RpcEndpoint rpcEndpoint) throws Exception {
    return this.rpcDispatcher.registerRpcEndpointRef(new RpcAddress(this.host, this.port, rpcEndpoint.name), rpcEndpoint);
  }

  public RpcEndpointRef registerRpcEndpointRef(RpcAddress rpcAddress) throws Exception {
    return this.rpcDispatcher.registerRpcEndpointRef(rpcAddress);
  }

  public List<RpcEndpointRef> getRegisteredRpcEndpointRefs() {
    return this.rpcDispatcher.getRegisteredRpcEndpointRefs();
  }

  public boolean isOutbound(RpcMessage message) {
    RpcAddress sender = message.getSender();
    return sender.host.equals(serverAddress.host) && sender.port == serverAddress.port;
  }

  public boolean isInbound(RpcMessage message) {
    RpcAddress receiver = message.getReceiver();
    return receiver.host.equals(serverAddress.host) && receiver.port == serverAddress.port;
  }

  public RpcServer getRpcServer() {
    return this.rpcServer;
  }

  public RpcDispatcher getRpcDispatcher() {
    return this.rpcDispatcher;
  }

  public RpcStreamingManager getRpcStreamingManager() {
    return this.streamingManager;
  }

  public RpcConf getRpcConf() {
    return this.rpcConf;
  }

  public void stop() {
    if (started) {
      this.rpcServer.stop();
      this.rpcDispatcher.stop();
    }
  }
}
