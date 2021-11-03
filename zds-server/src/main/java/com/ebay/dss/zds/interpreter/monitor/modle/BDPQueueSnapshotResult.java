package com.ebay.dss.zds.interpreter.monitor.modle;

import com.ebay.dss.zds.common.JsonUtil;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.validation.constraints.NotBlank;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by tatian on 2019-06-04.
 */
@NotThreadSafe
public class BDPQueueSnapshotResult {


    private boolean success;
    private String version;
    private List<BDPQueueSnapshot> result;
    private int count;

    public static class BDPQueueSnapshot {

        @NotBlank
        private String queueName;
        private String parentQueueName;
        private int submittedApps;
        private int runningApps;
        private int pendingApps;
        private long allocatedMB;
        private long pendingMB;
        private long availableMB;
        private long reservedMB;
        private int allocatedVCores;
        private int pendingVCores;
        private int allocatedContainers;
        private int pendingContainers;
        private double capacity;
        private double maxCapacity;
        private String nodeLabels;
        private double clusterUsagePercentage;
        private boolean isLeaf;
        private long snapTimestamp;
        private String site;

        public String getQueueName() {
            return queueName;
        }

        public void setQueueName(String queueName) {
            this.queueName = queueName;
        }

        public String getParentQueueName() {
            return parentQueueName;
        }

        public void setParentQueueName(String parentQueueName) {
            this.parentQueueName = parentQueueName;
        }

        public int getSubmittedApps() {
            return submittedApps;
        }

        public void setSubmittedApps(int submittedApps) {
            this.submittedApps = submittedApps;
        }

        public int getRunningApps() {
            return runningApps;
        }

        public void setRunningApps(int runningApps) {
            this.runningApps = runningApps;
        }

        public int getPendingApps() {
            return pendingApps;
        }

        public void setPendingApps(int pendingApps) {
            this.pendingApps = pendingApps;
        }

        public long getAllocatedMB() {
            return allocatedMB;
        }

        public void setAllocatedMB(long allocatedMB) {
            this.allocatedMB = allocatedMB;
        }

        public long getPendingMB() {
            return pendingMB;
        }

        public void setPendingMB(long pendingMB) {
            this.pendingMB = pendingMB;
        }

        public long getAvailableMB() {
            return availableMB;
        }

        public void setAvailableMB(long availableMB) {
            this.availableMB = availableMB;
        }

        public long getReservedMB() {
            return reservedMB;
        }

        public void setReservedMB(long reservedMB) {
            this.reservedMB = reservedMB;
        }

        public int getAllocatedVCores() {
            return allocatedVCores;
        }

        public void setAllocatedVCores(int allocatedVCores) {
            this.allocatedVCores = allocatedVCores;
        }

        public int getPendingVCores() {
            return pendingVCores;
        }

        public void setPendingVCores(int pendingVCores) {
            this.pendingVCores = pendingVCores;
        }

        public int getAllocatedContainers() {
            return allocatedContainers;
        }

        public void setAllocatedContainers(int allocatedContainers) {
            this.allocatedContainers = allocatedContainers;
        }

        public int getPendingContainers() {
            return pendingContainers;
        }

        public void setPendingContainers(int pendingContainers) {
            this.pendingContainers = pendingContainers;
        }

        public double getCapacity() {
            return capacity;
        }

        public void setCapacity(double capacity) {
            this.capacity = capacity;
        }

        public double getMaxCapacity() {
            return maxCapacity;
        }

        public void setMaxCapacity(double maxCapacity) {
            this.maxCapacity = maxCapacity;
        }

        public String getNodeLabels() {
            return nodeLabels;
        }

        public void setNodeLabels(String nodeLabels) {
            this.nodeLabels = nodeLabels;
        }

        public double getClusterUsagePercentage() {
            return clusterUsagePercentage;
        }

        public void setClusterUsagePercentage(double clusterUsagePercentage) {
            this.clusterUsagePercentage = clusterUsagePercentage;
        }

        public boolean isLeaf() {
            return isLeaf;
        }

        public void setLeaf(boolean leaf) {
            isLeaf = leaf;
        }

        public long getSnapTimestamp() {
            return snapTimestamp;
        }

        public void setSnapTimestamp(long snapTimestamp) {
            this.snapTimestamp = snapTimestamp;
        }

        public String getSite() {
            return site;
        }

        public void setSite(String site) {
            this.site = site;
        }

        public YarnQueue toYarnQueue() {
            return toYarnQueue(this);
        }

        static YarnQueue toYarnQueue(BDPQueueSnapshot bdpQueueSnapshot) {
            if (bdpQueueSnapshot == null) return null;
            YarnQueue yarnQueue = new YarnQueue();
            yarnQueue.setQueueName(bdpQueueSnapshot.getQueueName());
            yarnQueue.setAbsoluteMaxCapacity(bdpQueueSnapshot.getMaxCapacity());
            yarnQueue.setAbsoluteUsedCapacity(bdpQueueSnapshot.getClusterUsagePercentage());
            yarnQueue.setAbsoluteCapacity(bdpQueueSnapshot.getCapacity());
            yarnQueue.setNodeLabels(Arrays.asList(bdpQueueSnapshot.nodeLabels));
            return yarnQueue;
        }
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<BDPQueueSnapshot> getResult() {
        return result;
    }

    @Nullable
    public List<BDPQueueSnapshot> getLatestResult() {
        if (result == null || result.size() == 0) return null;
        return result
                .stream()
                .filter(snapshot -> StringUtils.isNotEmpty(snapshot.getQueueName()))
                .collect(Collectors
                        .groupingBy(BDPQueueSnapshot::getQueueName,
                                Collectors.maxBy(
                                        Comparator.comparingLong(BDPQueueSnapshot::getSnapTimestamp))))
                .values()
                .stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public void setResult(List<BDPQueueSnapshot> result) {
        this.result = result;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    @Nullable
    public BDPQueueSnapshot getFirstLatestSnapshot() {
        List<BDPQueueSnapshot> snapshots = getLatestResult();
        if (snapshots != null && snapshots.size() > 0) {
            snapshots.sort((l , r) -> {
                final long sub = r.getSnapTimestamp() - l.getSnapTimestamp();
                return sub > 0 ? 1 : (sub == 0 ? 0 : -1);
            });
            return snapshots.get(0);
        } else return null;
    }

    public static BDPQueueSnapshotResult fromJson(String json) {
        return JsonUtil.GSON.fromJson(json, BDPQueueSnapshotResult.class);
    }

    public String toJson() {
        return JsonUtil.GSON.toJson(this);
    }
}
