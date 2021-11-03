package com.ebay.dss.zds.interpreter.interpreters.imitation;

/**
 * Created by tatian on 2020-12-02.
 */
public interface Behavior {

  void perform();
  void stop();
  default int progress() {
    return 0;
  }
}
