package com.ebay.dss.zds.rpc;

/**
 * Created by tatian on 2020-09-16.
 */
public abstract class Clock {

  public static final Clock LOCAL = new Clock() {
    @Override
    public long getTime() {
      return System.currentTimeMillis();
    }
  };

  public static final Clock GLOBAL = new Clock() {
    @Override
    public long getTime() {
      throw new UnsupportedOperationException();
    }
  };

  public abstract long getTime();
}
