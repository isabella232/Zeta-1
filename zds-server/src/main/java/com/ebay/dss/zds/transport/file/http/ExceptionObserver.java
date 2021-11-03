package com.ebay.dss.zds.transport.file.http;

import io.netty.channel.ChannelHandlerContext;

import java.util.Optional;

/**
 * Created by tatian on 2019-11-01.
 */
public abstract class ExceptionObserver {

  public abstract void onException(ChannelHandlerContext ctx, Throwable throwable);

  public abstract Optional<Throwable> getObserved();

  public static class SimpleExceptionObserver extends ExceptionObserver{
    private Throwable throwable;
    @Override
    public void onException(ChannelHandlerContext ctx, Throwable throwable) {
      this.throwable = throwable;
    }

    @Override
    public Optional<Throwable> getObserved() {
      return Optional.ofNullable(throwable);
    }
  }
}
