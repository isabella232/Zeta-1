package com.ebay.dss.zds.rpc.serializer;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Created by tatian on 2020-10-10.
 */
public class RpcMessageCodec extends ByteToMessageCodec<Object> {

  private static final Logger LOG = LoggerFactory.getLogger(RpcMessageCodec.class);
  private final RpcMessageSerializer serializer;

  public RpcMessageCodec(List<Class> messages) {
    this.serializer = new kryoRpcMessageSerializer(messages);
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)
          throws Exception {
    if (in.readableBytes() < 4) {
      return;
    }

    in.markReaderIndex();
    int msgSize = in.readInt();

    if (in.readableBytes() < msgSize) {
      // Incomplete message in buffer.
      in.resetReaderIndex();
      return;
    }

    try {
      ByteBuffer nioBuffer = in.nioBuffer(in.readerIndex(), msgSize);
      Object msg = serializer.deserialize(nioBuffer);
      LOG.debug("Decoded message of type {} ({} bytes)",
              msg != null ? msg.getClass().getName() : msg, msgSize);
      out.add(msg);
    } finally {
      in.skipBytes(msgSize);
    }
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf buf)
          throws Exception {
    ByteBuffer msgData = serializer.serialize(msg);
    LOG.debug("Encoded message of type {} ({} bytes)", msg.getClass().getName(),
            msgData.remaining());

    buf.ensureWritable(msgData.remaining() + 4);
    buf.writeInt(msgData.remaining());
    buf.writeBytes(msgData);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    cause.printStackTrace();
    super.exceptionCaught(ctx, cause);
  }

}
