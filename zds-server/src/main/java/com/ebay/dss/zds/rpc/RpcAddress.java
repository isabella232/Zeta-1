package com.ebay.dss.zds.rpc;

import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Created by tatian on 2020-09-06.
 */
public class RpcAddress implements Serializable {

  public static final String CONTROLLER = "";
  public static final String STREAMING_MANAGER = RpcStreamingManager.class.getSimpleName();

  public final String host;
  public final int port;
  public final String name;
  public final int hashCode;

  public RpcAddress(String host, int port, String name) {
    assert StringUtils.isNotEmpty(host) && port >= 0;
    this.host = host;
    this.port = port;
    this.name = name != null ? name : CONTROLLER;
    this.hashCode = generateHashCode();
  }

  public RpcAddress(int port, String name) throws UnknownHostException {
    this(InetAddress.getLocalHost().getHostName(), port, name);
  }

  public RpcAddress(int port) throws UnknownHostException {
    this(InetAddress.getLocalHost().getHostName(), port, CONTROLLER);
  }

  private int generateHashCode() {
    return toString().hashCode();
  }

  @Override
  public String toString() {
    return (host + ":" + port + ":" + name);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof RpcAddress) {
      RpcAddress other = (RpcAddress) obj;
      return this.host.equals(other.host) && this.port == other.port && this.name.equals(other.name);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  public static RpcAddress local(int port, String name) throws UnknownHostException {
    return new RpcAddress(port, name);
  }

  public RpcAddress getControllerRpcAddress() {
    return new RpcAddress(this.host, this.port, CONTROLLER);
  }
  public RpcAddress getStreamingRpcAddress() {
    return new RpcAddress(this.host, this.port, STREAMING_MANAGER);
  }

  public static class RpcAddressSource extends RpcAddress {

    public final long sourceId;

    public RpcAddressSource(long sourceId, String host, int port, String name) {
      super(host, port, name);
      this.sourceId = sourceId;
    }

    public RpcAddressSource(long sourceId, RpcAddress address) {
      super(address.host, address.port, address.name);
      this.sourceId = sourceId;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof RpcAddressSource) {
        RpcAddressSource other = (RpcAddressSource) obj;
        return this.sourceId == other.sourceId && super.equals(obj);
      } else {
        return false;
      }
    }

    @Override
    public String toString() {
      return super.toString() + ":" + this.sourceId;
    }

    @Override
    public int hashCode() {
      return hashCode;
    }
  }

  public static class RpcAddressSet implements Serializable {

    private RpcAddress[] addressList;
    public RpcAddressSet(List<RpcAddress> list) {
      this.addressList = (RpcAddress[]) list.toArray();
    }
  }

}
