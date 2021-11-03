package com.ebay.dss.zds.interpreter.interpreters.imitation.behavior;

import com.ebay.dss.zds.common.ThreadUtils;
import com.ebay.dss.zds.interpreter.interpreters.imitation.Behavior;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by tatian on 2020-12-02.
 */
public class Sleep implements Behavior {

  public final long sleepTime;
  public final LinkedBlockingQueue<Object> sync;

  public Sleep(long sleepTime) {
    this.sleepTime = sleepTime;
    this.sync = new LinkedBlockingQueue<>();
  }

  public void perform() {
    if (sleepTime > 0) {
      try {
        this.sync.poll(sleepTime, TimeUnit.MILLISECONDS);
      } catch (Exception ex) {
        // stop the perform
      }
      // ThreadUtils.sleep(sleepTime);
    }
  }

  public void stop() {
      this.sync.offer(new Object());
  }
}
