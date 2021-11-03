package com.ebay.dss.zds.interpreter.interpreters.imitation.behavior;

import com.ebay.dss.zds.interpreter.interpreters.imitation.Behavior;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by tatian on 2020-12-02.
 */
public class Loop implements Behavior {

  public final long totalLoop;
  public final Sleep sleep;
  public final Behavior inEachLoopDo;
  private final AtomicLong cnt = new AtomicLong(0);
  private final AtomicBoolean stopped = new AtomicBoolean(false);

  public Loop(long totalLoop) {
    this(totalLoop, 0L, Do.DoNothing());
  }

  public Loop(long totalLoop, long interval) {
    this(totalLoop, interval, Do.DoNothing());
  }

  public Loop(long totalLoop, long interval, Behavior inEachLoopDo) {
    this.totalLoop = totalLoop;
    this.sleep = new Sleep(interval);
    this.inEachLoopDo = inEachLoopDo;
  }

  public void perform() {
    cnt.set(0);
    while (cnt.getAndIncrement() < totalLoop && !stopped.get()) {
      this.inEachLoopDo.perform();
      this.sleep.perform();
    }
  }

  @Override
  public int progress() {
    return totalLoop > 0 ? Math.round(cnt.get() * 100 / totalLoop) : 0;
  }

  public void stop() {
    this.stopped.set(true);
  }
}
