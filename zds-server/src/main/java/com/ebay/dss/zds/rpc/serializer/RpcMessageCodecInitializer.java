package com.ebay.dss.zds.rpc.serializer;

import com.ebay.dss.zds.common.ClassUtil;
import com.ebay.dss.zds.common.LazyObject;
import com.ebay.dss.zds.rpc.RpcConf;
import com.ebay.dss.zds.rpc.message.RpcMessage;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by tatian on 2020-10-10.
 */
public class RpcMessageCodecInitializer {

  protected static final Logger logger = LoggerFactory.getLogger(RpcMessageCodecInitializer.class);

  private RpcConf rpcConf;
  private static final LazyObject<List<Class>> messages = LazyObject.lazy(() -> listRpcMessageClasses(true));

  public RpcMessageCodecInitializer(RpcConf conf) {
    this.rpcConf = conf;
    this.setup();
  }

  private void setup() {
    messages.initIf(rpcConf.getRpcMessageCodec().equals(RpcMessageCodec.class.getName()));
  }

  public static List<Class> listRpcMessageClasses(boolean printOut) {
    List<Class> classes;
    try {
      classes = ClassUtil.getClassFromPackage(RpcMessage.class.getPackage().getName())
              .stream()
              .filter((Class clazz) -> {
                String className = clazz.getName();
                return !className.toLowerCase().endsWith("test") && clazz.getCanonicalName() != null;
              }).collect(Collectors.toList());
    } catch (IOException ex) {
      ex.printStackTrace();
      classes = new ArrayList<>();
    }
    if (printOut) {
      String allMessage = classes
              .stream()
              .map(Class::getCanonicalName)
              .map((String className) -> className + ".class")
              .collect(Collectors.joining(",\n"));
      System.out.println("=========================== Kryo class registered ===========================");
      System.out.println(allMessage);
      System.out.println("=========================== Kryo class registered ===========================");
    }
    return classes;
  }

  public ChannelPipeline addCodec(ChannelPipeline pipeline) {
    String codecClass = rpcConf.getRpcMessageCodec();

    logger.info("Adding codec: " + codecClass);
    if (codecClass.equals(RpcMessageCodec.class.getName())) {
      return pipeline.addLast(createRpcMessageCodec());
    } else if (codecClass.equals(ObjectEncoder.class.getName()) || codecClass.equals(ObjectDecoder.class.getName())) {
      return addDefaultCodec(pipeline);
    } else {
      logger.info("No valid codec set, use Object codec");
      return addDefaultCodec(pipeline);
    }
  }

  private RpcMessageCodec createRpcMessageCodec() {
    return new RpcMessageCodec(messages.get());
  }

  private ChannelPipeline addDefaultCodec(ChannelPipeline pipeline) {
    return pipeline
            .addLast(new ObjectEncoder())
            .addLast(new ObjectDecoder(Integer.MAX_VALUE, ClassResolvers.cacheDisabled(this.getClass().getClassLoader())));
  }

}
