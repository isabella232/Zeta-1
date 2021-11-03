package com.ebay.dss.zds.model.schedule;

public class DependencyTable {

  String tableName;
  boolean isWait;
  int timeLag;

  public String getTableName() {
    return tableName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  public boolean isWait() {
    return isWait;
  }

  public void setWait(boolean wait) {
    isWait = wait;
  }

  public int getTimeLag() {
    return timeLag;
  }

  public void setTimeLag(int timeLag) {
    this.timeLag = timeLag;
  }
}
