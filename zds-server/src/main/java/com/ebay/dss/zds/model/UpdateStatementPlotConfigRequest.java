package com.ebay.dss.zds.model;

public class UpdateStatementPlotConfigRequest {
  private long id;
  private String plotConfig;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getPlotConfig() {
    return plotConfig;
  }

  public void setPlotConfig(String plotConfig) {
    this.plotConfig = plotConfig;
  }
}
