package com.ebay.dss.zds.interpreter.lifecycle;

import java.util.Properties;

/**
 * Created by tatian on 2018/6/11.
 */
public class NumberFilter implements Filter {

  private int maxNumber = 6;

  private int cnt = 1;

  private static final String MAX_NUMBER_PREFIX = "zds.interpreter.lifecycle.monitor.filter.numberFilter.maxNumber";

  public boolean filtered() {
    if (cnt >= maxNumber) {
      return true;
    } else {
      return false;
    }
  }

  public void apply(Properties prop) {
    maxNumber = Integer.valueOf(prop.getProperty(MAX_NUMBER_PREFIX));
  }

  public void addRule() {
    cnt++;
  }

  public void removeRule() {
    if (cnt > 0) {
      cnt--;
    }
  }

  public Filter create() {
    return new NumberFilter().setMaxNumber(maxNumber);
  }

  public NumberFilter setMaxNumber(Integer num) {
    this.maxNumber = num;
    return this;
  }

  public String explain() {
    return "The number of interpreters(including background interpreters) is restricted to no more than " + maxNumber;
  }

  public String introduce() {
    return "Current number of interpreters: " + cnt;
  }
}
