package com.ebay.dss.zds.rpc.message;

import com.ebay.dss.zds.rpc.RpcAddress;

import java.io.Serializable;

/**
 * Created by tatian on 2020-09-16.
 */
public class RpcAliveDetect extends RpcRequestMessage<RpcAliveDetect.DetectSend, RpcAliveDetect.DetectBack> {

  public RpcAliveDetect(RpcAddress sender, RpcAddress receiver) {
    super(new DetectSend(), sender.getControllerRpcAddress(), receiver.getControllerRpcAddress());
  }


  public static class DetectSend implements Serializable {
    public final long sendTime;

    public DetectSend(long sendTime) {
      this.sendTime = sendTime;
    }

    public DetectSend() {
      this(System.currentTimeMillis());
    }
  }

  public static class DetectBack extends DetectSend {

    public DetectBack(long sendTime) {
      super(sendTime);
    }

    public DetectBack() {
      super();
    }

  }
}
