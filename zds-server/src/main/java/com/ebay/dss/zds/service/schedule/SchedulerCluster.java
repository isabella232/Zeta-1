package com.ebay.dss.zds.service.schedule;

import com.google.common.collect.Maps;

import java.util.Map;

public enum SchedulerCluster {
  apollo_rno(14),
  hermes(16),
  ares(2),
  hercules(10),
  hopper(90),
  numozart(91),
  mozart(91);

  int clusterId;

  SchedulerCluster(int clusterId) {
    this.clusterId = clusterId;
  }

  static Map<Integer, SchedulerCluster> clusterMap = Maps.newLinkedHashMap();

  static {
    for (SchedulerCluster cluster : SchedulerCluster.values()) {
      clusterMap.put(cluster.clusterId, cluster);
    }
  }

  public static SchedulerCluster getClusterById(int clusterId) {
    return clusterMap.get(clusterId);
  }
}
