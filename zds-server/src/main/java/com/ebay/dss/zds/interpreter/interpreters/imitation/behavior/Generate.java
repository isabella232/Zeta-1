package com.ebay.dss.zds.interpreter.interpreters.imitation.behavior;

import com.ebay.dss.zds.interpreter.interpreters.imitation.Behavior;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Created by tatian on 2020-12-02.
 */
public class Generate implements Behavior {

  public final long num;
  public final Sleep keep;
  public final Supplier<Object> generator;
  private final AtomicLong cnt = new AtomicLong(0);
  private final AtomicBoolean stopped = new AtomicBoolean(false);

  public Generate(long num, long keepTime) {
    this(num, keepTime,() -> UUID.randomUUID().toString());
  }

  public Generate(long num, long keepTime, Supplier<Object> generator) {
    this.num = num;
    this.keep = new Sleep(keepTime);
    this.generator = generator;
  }

  public void perform() {
    cnt.set(0);
    List<Object> objs = new ArrayList<>();
    while (cnt.getAndIncrement() < num && !stopped.get()) {
      objs.add(generator.get());
    }
    keep.perform();
    objs.clear();
  }

  @Override
  public int progress() {
    return num > 0 ? Math.round(cnt.get() * 100 / num) : 0;
  }

  public void stop() {
    this.stopped.set(true);
  }
}
