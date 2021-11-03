package com.ebay.dss.zds.rpc.utils;

import java.util.stream.IntStream;

/**
 * Created by tatian on 2020-04-28.
 */
public class FunctionUtils {

  public static void repeat(int num, Runnable runnable) {
    IntStream.rangeClosed(1, num).forEach(i -> runnable.run());
  }
}
