package com.ebay.dss.zds.interpreter.monitor.modle;

import java.util.List;

/**
 * Created by tatian on 2019-10-29.
 */
public class ClusterAccessInfo {

  private int clusterId;
  private String clusterName;
  private String status; // NO_ACCESS/HAS_ACCESS
  private String reason;
  private List<String> ownedQueues;
  private List<String> ownedDirectories;

  public int getClusterId() {
    return clusterId;
  }

  public void setClusterId(int clusterId) {
    this.clusterId = clusterId;
  }

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public List<String> getOwnedQueues() {
    return ownedQueues;
  }

  public void setOwnedQueues(List<String> ownedQueues) {
    this.ownedQueues = ownedQueues;
  }

  public List<String> getOwnedDirectories() {
    return ownedDirectories;
  }

  public void setOwnedDirectories(List<String> ownedDirectories) {
    this.ownedDirectories = ownedDirectories;
  }

  public boolean isAccessible() {
    return "HAS_ACCESS".equals(status);
  }
}
