package com.ebay.dss.zds.model.schedule;


import java.util.List;

public class SchedulerDependencySignal {

  private boolean enabled;
  private List<String> signalTables;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public List<String> getSignalTables() {
    return signalTables;
  }

  public void setSignalTables(List<String> signalTables) {
    this.signalTables = signalTables;
  }
}
