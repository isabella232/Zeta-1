package com.ebay.dss.zds.interpreter.interpreters.imitation.behavior;

import com.ebay.dss.zds.interpreter.interpreters.imitation.Behavior;

/**
 * Created by tatian on 2020-12-02.
 */
public class Do implements Behavior {

  private final Runnable delegate;

  public Do(Runnable delegate) {
    this.delegate = delegate;
  }

  public void perform() {
    this.delegate.run();
  }
  public void stop() {
  }

  public static Do DoNothing() {
    return new Do(() -> {});
  }
}
