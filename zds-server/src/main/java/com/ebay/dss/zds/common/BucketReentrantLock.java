package com.ebay.dss.zds.common;

import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * Created by tatian on 2020-08-27.
 */
@ThreadSafe
public class BucketReentrantLock {

  private final ReentrantLock[] reentrantLocks;

  public BucketReentrantLock(int size) {
    assert size > 0;
    this.reentrantLocks = new ReentrantLock[size];
    IntStream.range(0, size).forEach(i -> this.reentrantLocks[i] = new ReentrantLock());
  }

  public int size(){
    return this.reentrantLocks.length;
  }

  public final ReentrantLock getLock(int index) {
    return this.reentrantLocks[index];
  }

  public final ReentrantLock getLock(String pattern) {
    assert pattern != null;
    int hash = pattern.hashCode();
    int index = Math.abs(hash) % size();
    return getLock(index);
  }

  public void runWithLock(String pattern, Runnable runnable) {
    ReentrantLock lock = getLock(pattern);
    try {
      lock.lock();
      runnable.run();
    } finally {
      lock.unlock();
    }
  }

}
