package com.ebay.dss.zds.common;

import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Created by tatian on 2020-10-10.
 */
@ThreadSafe
public class LazyObject<T> {

  private volatile T object;
  private final Supplier<T> supplier;
  private final ReentrantLock constructLock;

  public LazyObject(Supplier<T> supplier) {
    this.supplier = supplier;
    this.constructLock = new ReentrantLock();
  }

  public T get() {
    if (object != null) {
      return object;
    } else {
      try {
        constructLock.lock();
        if (this.object == null) {
          this.object = supplier.get();
        }
        return this.object;
      } finally {
        constructLock.unlock();
      }
    }
  }

  public void initIf(boolean condition) {
    if (condition) {
      this.get();
    }
  }

  public static <T> LazyObject<T> lazy(Supplier<T> supplier) {
    return new LazyObject<>(supplier);
  }
}
