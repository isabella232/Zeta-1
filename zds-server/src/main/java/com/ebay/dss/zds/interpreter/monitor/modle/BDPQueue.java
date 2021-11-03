package com.ebay.dss.zds.interpreter.monitor.modle;

public class BDPQueue {
    private int clusterId;
    private String clusterName;
    private String queueName;
    private String parentQueueName;
    private String submitApps;
    private int budgetGroupId;
    private String budgetGroup;
    private String info;
    private int version;
    private boolean leafQueue;
    private boolean defaultQueue;
    private String state;
    private double absCapacity;
    private double maxAbsCapacity;
    private String nodeLabel; // highmemory
    private Object labelInfo;
    private boolean owned;
    private boolean accessible;
    private boolean applied;
    private boolean sameBudget;
    private String owners;
    private Object subQueues;

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

    public String getSubmitApps() {
        return submitApps;
    }

    public void setSubmitApps(String submitApps) {
        this.submitApps = submitApps;
    }

    public int getBudgetGroupId() {
        return budgetGroupId;
    }

    public void setBudgetGroupId(int budgetGroupId) {
        this.budgetGroupId = budgetGroupId;
    }

    public String getBudgetGroup() {
        return budgetGroup;
    }

    public void setBudgetGroup(String budgetGroup) {
        this.budgetGroup = budgetGroup;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public boolean isLeafQueue() {
        return leafQueue;
    }

    public void setLeafQueue(boolean leafQueue) {
        this.leafQueue = leafQueue;
    }

    public boolean isDefaultQueue() {
        return defaultQueue;
    }

    public void setDefaultQueue(boolean defaultQueue) {
        this.defaultQueue = defaultQueue;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public double getAbsCapacity() {
        return absCapacity;
    }

    public void setAbsCapacity(double absCapacity) {
        this.absCapacity = absCapacity;
    }

    public double getMaxAbsCapacity() {
        return maxAbsCapacity;
    }

    public void setMaxAbsCapacity(double maxAbsCapacity) {
        this.maxAbsCapacity = maxAbsCapacity;
    }

    public String getNodeLabel() {
        return nodeLabel;
    }

    public void setNodeLabel(String nodeLabel) {
        this.nodeLabel = nodeLabel;
    }

    public Object getLabelInfo() {
        return labelInfo;
    }

    public void setLabelInfo(Object labelInfo) {
        this.labelInfo = labelInfo;
    }

    public boolean isOwned() {
        return owned;
    }

    public void setOwned(boolean owned) {
        this.owned = owned;
    }

    public boolean isAccessible() {
        return accessible;
    }

    public void setAccessible(boolean accessible) {
        this.accessible = accessible;
    }

    public boolean isApplied() {
        return applied;
    }

    public void setApplied(boolean applied) {
        this.applied = applied;
    }

    public boolean isSameBudget() {
        return sameBudget;
    }

    public void setSameBudget(boolean sameBudget) {
        this.sameBudget = sameBudget;
    }

    public String getOwners() {
        return owners;
    }

    public void setOwners(String owners) {
        this.owners = owners;
    }

    public Object getSubQueues() {
        return subQueues;
    }

    public void setSubQueues(Object subQueues) {
        this.subQueues = subQueues;
    }
}
