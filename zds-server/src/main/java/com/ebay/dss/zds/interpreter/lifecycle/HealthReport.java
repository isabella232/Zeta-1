package com.ebay.dss.zds.interpreter.lifecycle;

/**
 * Created by tatian on 2020-08-04.
 */
public class HealthReport {

  private volatile boolean isHealth;
  private volatile long last;

  public HealthReport(boolean health) {
    this.isHealth = health;
    this.last = System.currentTimeMillis();
  }

  public boolean isHealth() {
    return this.isHealth;
  }

  public void setHealth(boolean health) {
    this.isHealth = health;
    this.last = System.currentTimeMillis();
  }
}
