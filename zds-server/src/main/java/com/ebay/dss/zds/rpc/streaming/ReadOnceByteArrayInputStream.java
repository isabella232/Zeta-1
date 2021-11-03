package com.ebay.dss.zds.rpc.streaming;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by tatian on 2020-09-11.
 */
public class ReadOnceByteArrayInputStream extends InputStream {

  private volatile boolean canInsert = true;
  private volatile boolean canRead = true;
  private volatile boolean hasInput = true;
  private AtomicBoolean hasConsumer = new AtomicBoolean(false);

  private final boolean tryBest;

  public static final int MAX_POLL_TIMEOUT_SECS = 120;
  public final int pollTimeout;

  private LinkedBlockingQueue<byte[]> bufQueue;
  private volatile byte[] currentBuf;
  private volatile int pos;

  private static final byte[] STOP_FETCH = "STOP_FETCH".getBytes();

  private ReentrantLock lock = new ReentrantLock();
  private ReentrantLock insertLock = new ReentrantLock();

  public ReadOnceByteArrayInputStream() {
    this(MAX_POLL_TIMEOUT_SECS, true);
  }

  public ReadOnceByteArrayInputStream(boolean tryBest) {
    this(MAX_POLL_TIMEOUT_SECS, tryBest);
  }

  public ReadOnceByteArrayInputStream(int pollTimeout) {
    this(pollTimeout, true);
  }

  public ReadOnceByteArrayInputStream(int pollTimeout, boolean tryBest) {
    this.bufQueue = new LinkedBlockingQueue<>();
    this.pollTimeout = pollTimeout;
    this.pos = 0;
    this.tryBest = tryBest;
  }

  public byte[] getCurrentBuf() {
    byte[] copied = new byte[currentBuf.length];
    System.arraycopy(currentBuf, 0, copied, 0, currentBuf.length);
    return copied;
  }

  public int getPos() {
    return pos;
  }

  private byte[] timeoutPoll() {
    try {
      byte[] polled = bufQueue.poll(pollTimeout, TimeUnit.SECONDS);
      if (polled != null && polled.equals(STOP_FETCH)) {
        return null;
      } else return polled;
    } catch (InterruptedException ex) {
      return null;
    }
  }

  private byte[] tryFetchNewBuf() {
    if (bufQueue.size() > 0 || hasInput) {
      return timeoutPoll();
    } else return null;
  }

  public boolean insert(byte[] buf) {
    try {
      insertLock.lock();
      return canInsert && this.bufQueue.offer(buf);
    } finally {
      insertLock.unlock();
    }
  }

  @Override
  public int read() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int read(byte b[]) {
    hasConsumer.compareAndSet(false, true);
    try {
      lock.lock();
      if (!canRead) return -1;
      int expectedSize = b.length;

      if (currentBuf == null) {
        currentBuf = tryFetchNewBuf();
        if (currentBuf == null) {
          return -1;
        } else {
          pos = 0;
        }
      }

      int currentBufRemaining = currentBuf.length - pos;
      if (currentBufRemaining >= expectedSize) {
        System.arraycopy(currentBuf, pos, b, 0, expectedSize);
        pos += expectedSize;
        return expectedSize;
      } else {
        if (currentBufRemaining > 0 && !tryBest) {
          System.arraycopy(currentBuf, pos, b, 0, currentBufRemaining);
          pos += currentBufRemaining;
          return currentBufRemaining;
        } else {
          if (!hasInput && bufQueue.size() == 0) {
            if (currentBufRemaining > 0) {
              System.arraycopy(currentBuf, pos, b, 0, currentBufRemaining);
              pos += currentBufRemaining;
              return currentBufRemaining;
            } else return -1;
          } else {
            int bPos = 0;
            while (bPos < expectedSize) {
              int leftBufSize = currentBuf.length - pos;
              if (leftBufSize > 0) {
                int leftToFeed = expectedSize - bPos;
                int len = Math.min(leftBufSize, leftToFeed);
                System.arraycopy(currentBuf, pos, b, bPos, len);
                pos += len;
                bPos += len;
                if (!tryBest) return bPos;
              } else {
                currentBuf = tryFetchNewBuf();
                if (currentBuf == null) {
                  if (bPos > 0) {
                    return bPos;
                  } else return -1;
                } else {
                  pos = 0;
                }
              }
            }
            return bPos;
          }
        }
      }

    } finally {
      lock.unlock();
    }
  }

  @Override
  public int read(byte b[], int off, int len) {
    throw new UnsupportedOperationException();
  }

  @Override
  public long skip(long n) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int available() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean markSupported() {
    return false;
  }

  @Override
  public void mark(int readAheadLimit) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void reset() {
    try {
      lock.lock();
      this.bufQueue.clear();
      this.currentBuf = null;
      this.pos = 0;
      this.canInsert = true;
      this.hasInput = true;
      this.canRead = true;
    } finally {
      lock.unlock();
    }
  }

  public void markedNoInput() {
    try {
      insertLock.lock();
      this.hasInput = false;
      this.canInsert = false;
      this.bufQueue.offer(STOP_FETCH);
    } finally {
      insertLock.unlock();
    }
  }

  public void markedHasInput() {
    try {
      insertLock.lock();
      this.hasInput = true;
    } finally {
      insertLock.unlock();
    }
  }

  public boolean hasInput() {
    return this.hasInput;
  }

  public boolean hasConsumer() {
    return this.hasConsumer.get();
  }

  public void markedCanRead(boolean canRead) {
    try {
      lock.lock();
      this.canRead = canRead;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void close() throws IOException {
    try {
      lock.lock();
      markedNoInput();
      this.canRead = false;
      this.bufQueue.clear();
    } finally {
      lock.unlock();
    }
  }
}
