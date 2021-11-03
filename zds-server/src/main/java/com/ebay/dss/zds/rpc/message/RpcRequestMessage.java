package com.ebay.dss.zds.rpc.message;

import com.ebay.dss.zds.rpc.RpcAddress;
import com.ebay.dss.zds.rpc.RpcEnv;

import java.io.Serializable;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by tatian on 2020-09-07.
 */
public class RpcRequestMessage<T extends Serializable, V extends Serializable> extends RpcMessage<T> {

  // This is only for client side, we don't want it be called in server side
  private transient FutureResult<V> futureResult;

  public RpcRequestMessage(T message, RpcAddress sender, RpcAddress receiver) {
    super(message, sender, receiver);
    futureResult = new FutureResult<>(this);
  }

  public void setResult(V result) {
    futureResult.setSuccess(result);
  }

  public void setFailure(Throwable result) {
    futureResult.setFailure(result);
  }

  public V getSync() throws Exception {
    return this.futureResult.get();
  }

  public FutureResult<V> get() {
    return futureResult;
  }

  public boolean isCanelled() {
    return this.futureResult.isCancelled();
  }

  public boolean cancel(boolean mayInterruptIfRunning, RpcEnv rpcEnv) {
    if (mayInterruptIfRunning) {
      // tell both local to cancel it?
      RpcCallCancel callCancel = new RpcCallCancel(id, getSender(), getReceiver());
      RpcMessage message = rpcEnv.getRpcDispatcher().tryAbortLocalMessage(id, getReceiver(), true);
      if (message == null) rpcEnv.ask(callCancel);
    }
    return this.futureResult.cancel(mayInterruptIfRunning);
  }

  public long cancelTime() {
    return this.futureResult.cancelTime;
  }

  public long lastUpdateTime() {
    return this.futureResult.lastUpdateTime;
  }

  /**
   * The real cancel implementation is a fake cancel
   * it only set the status in the instance itself and reject the back result
   */
  public static class FutureResult<V> implements Serializable, Future<V> {

    private final RpcRequestMessage message;
    private transient final CountDownLatch latch = new CountDownLatch(1);
    private AtomicReference<V> result = new AtomicReference<>();
    private AtomicReference<Throwable> throwable = new AtomicReference<>();
    private volatile AtomicBoolean canceled = new AtomicBoolean(false);
    private ReentrantLock lock = new ReentrantLock();

    private volatile long cancelTime = -1;
    private volatile long lastUpdateTime = -1;

    public FutureResult(RpcRequestMessage message) {
      this.message = message;
    }

    // this cancel only make the client side exit the waiting
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      try {
        lock.lock();
        if (!isDone()) {
          canceled.compareAndSet(false, true);
          message.setInTransport();
          long now = System.currentTimeMillis();
          cancelTime = now;
          lastUpdateTime = now;
          latch.countDown();
        }
        return canceled.get();
      } finally {
        lock.unlock();
      }
    }

    @Override
    public boolean isCancelled() {
      return canceled.get();
    }

    @Override
    public boolean isDone() {
      return canceled.get() || result.get() != null || throwable.get() != null;
    }

    public boolean isSuccess() {
      return this.result.get() != null;
    }

    public boolean isFailure() {
      return this.throwable.get() != null;
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {

      // if the message is not been posted
      if (!message.inTransport()) {
        return null;
      }

      while (!isDone()) {
        this.latch.await();
      }

      if (canceled.get()) throw new InterruptedException("This result has been canceled");
      if (isFailure()) throw new ExecutionException(this.throwable.get());
      return result.get();
    }

    @Override
    public V get(long timeout, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {

      // if the message is not been posted
      if (!message.inTransport()) {
        return null;
      }

      while (!isDone()) {
        this.latch.await(timeout, timeUnit);
        if (!isDone()) throw new TimeoutException("Result get timeout within(s): " + timeUnit.toSeconds(timeout));
      }

      if (canceled.get()) throw new InterruptedException("This result has been canceled");
      if (isFailure()) throw new ExecutionException(this.throwable.get());
      return result.get();
    }

    public void setSuccess(V v) {
      this.setResult(v);
    }

    // when canceled a throwable won't send back just to remove the un replied box
    public void setFailure(Throwable throwable) {
      try {
        lock.lock();
        if (!canceled.get()) {
          this.throwable.set(throwable);
          message.setInTransport();
          lastUpdateTime = System.currentTimeMillis();
          this.latch.countDown();
        }
      } finally {
        lock.unlock();
      }
    }

    public void setResult(V v) {
      try {
        lock.lock();
        if (!canceled.get()) {
          this.result.set(v);
          message.setInTransport();
          lastUpdateTime = System.currentTimeMillis();
          this.latch.countDown();
        }
      } finally {
        lock.unlock();
      }
    }
  }

}
