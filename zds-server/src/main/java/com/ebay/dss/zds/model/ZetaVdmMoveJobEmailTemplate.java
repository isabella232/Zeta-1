package com.ebay.dss.zds.model;

import java.util.List;

public class ZetaVdmMoveJobEmailTemplate extends ZetaEmailTemplate {

  private String targetDb;
  private String sourceDb;
  private List<String> overrideTables;

  public String getTargetDb() {
    return targetDb;
  }

  public void setTargetDb(String targetDb) {
    this.targetDb = targetDb;
  }

  public String getSourceDb() {
    return sourceDb;
  }

  public void setSourceDb(String sourceDb) {
    this.sourceDb = sourceDb;
  }

  public List<String> getOverrideTables() {
    return overrideTables;
  }

  public void setOverrideTables(List<String> overrideTables) {
    this.overrideTables = overrideTables;
  }
}
