package com.ebay.dss.zds.common;

import java.util.function.Supplier;

/**
 * Created by tatian on 2020-04-28.
 */
public class ThreadUtils {

  public static void sleep(long interval) {
    try {
      Thread.sleep(interval);
    } catch (InterruptedException ex) {
      ex.printStackTrace();
    }
  }

  public static <T> T withClassLoaderRestore(Supplier<T> supplier) {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    try {
      return supplier.get();
    } finally {
      Thread.currentThread().setContextClassLoader(classLoader);
    }
  }
}
