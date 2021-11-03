package com.ebay.dss.zds.model.schedule;


import java.util.List;

public class SchedulerDependency {

  private boolean enabled;
  private int waitingHrs;
  private List<DependencyTable> dependencyTables;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public int getWaitingHrs() {
    return waitingHrs;
  }

  public void setWaitingHrs(int waitingHrs) {
    this.waitingHrs = waitingHrs;
  }

  public List<DependencyTable> getDependencyTables() {
    return dependencyTables;
  }

  public void setDependencyTables(List<DependencyTable> dependencyTables) {
    this.dependencyTables = dependencyTables;
  }

}
