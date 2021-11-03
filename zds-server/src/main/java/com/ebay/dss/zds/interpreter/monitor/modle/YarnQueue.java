package com.ebay.dss.zds.interpreter.monitor.modle;

import com.ebay.dss.zds.common.JsonUtil;
import com.ebay.dss.zds.common.NumberUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class YarnQueue {

    private String type;
    private String queueName;
    private double capacity;
    private double usedCapacity;
    private double maxCapacity;
    private double absoluteCapacity;
    private double absoluteMaxCapacity;
    private double absoluteUsedCapacity;
    private double numApplications;
    private String state;
    private QueueElement queues;
    private List<String> nodeLabels;
    private int numActiveApplications;
    private int numPendingApplications;
    private int numContainers;
    private int maxApplications;
    private int maxApplicationsPerUser;
    private int userLimit;
    // Map<String, Double>
    private Object users;
    private double usedPct = 999.0;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getCapacity() {
        return capacity;
    }

    public void setCapacity(double capacity) {
        this.capacity = capacity;
    }

    public double getUsedCapacity() {
        return usedCapacity;
    }

    public void setUsedCapacity(double usedCapacity) {
        this.usedCapacity = usedCapacity;
    }

    public double getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(double maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public double getAbsoluteCapacity() {
        return absoluteCapacity;
    }

    public void setAbsoluteCapacity(double absoluteCapacity) {
        this.absoluteCapacity = absoluteCapacity;
    }

    public double getAbsoluteMaxCapacity() {
        return absoluteMaxCapacity;
    }

    public void setAbsoluteMaxCapacity(double absoluteMaxCapacity) {
        this.absoluteMaxCapacity = absoluteMaxCapacity;
    }

    public double getAbsoluteUsedCapacity() {
        return absoluteUsedCapacity;
    }

    public void setAbsoluteUsedCapacity(double absoluteUsedCapacity) {
        this.absoluteUsedCapacity = absoluteUsedCapacity;
    }

    public double getNumApplications() {
        return numApplications;
    }

    public void setNumApplications(double numApplications) {
        this.numApplications = numApplications;
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public QueueElement getQueues() {
        return queues;
    }

    public void setQueues(QueueElement queues) {
        this.queues = queues;
    }

    public List<String> getNodeLabels() {
        return nodeLabels;
    }

    public void setNodeLabels(List<String> nodeLabels) {
        this.nodeLabels = nodeLabels;
    }

    public int getNumActiveApplications() {
        return numActiveApplications;
    }

    public void setNumActiveApplications(int numActiveApplications) {
        this.numActiveApplications = numActiveApplications;
    }

    public int getNumPendingApplications() {
        return numPendingApplications;
    }

    public void setNumPendingApplications(int numPendingApplications) {
        this.numPendingApplications = numPendingApplications;
    }

    public int getNumContainers() {
        return numContainers;
    }

    public void setNumContainers(int numContainers) {
        this.numContainers = numContainers;
    }

    public int getMaxApplications() {
        return maxApplications;
    }

    public void setMaxApplications(int maxApplications) {
        this.maxApplications = maxApplications;
    }

    public int getMaxApplicationsPerUser() {
        return maxApplicationsPerUser;
    }

    public void setMaxApplicationsPerUser(int maxApplicationsPerUser) {
        this.maxApplicationsPerUser = maxApplicationsPerUser;
    }

    public int getUserLimit() {
        return userLimit;
    }

    public void setUserLimit(int userLimit) {
        this.userLimit = userLimit;
    }

    public Object getUsers() {
        return users;
    }

    public void setUsers(Object users) {
        if (Objects.nonNull(users) && users instanceof Map) {
            // do a defense copy and remove null key for map type
            Map<Object, Object> origin = (Map) users;
            Map<Object, Object> copy = new HashMap<>();
            origin.keySet()
                    .stream()
                    .filter(Objects::nonNull)
                    .forEach(k -> {
                        Object v = origin.get(k);
                        copy.put(k, v);
                    });
            this.users = copy;
        } else {
            this.users = users;
        }
    }

    public double getUsedPct() {
        if(usedPct == 999.0){
            if (absoluteMaxCapacity == 0) usedPct = 0.0;
            else usedPct = NumberUtils.afterDecimalPoint(absoluteUsedCapacity/absoluteMaxCapacity,2);
        }
        return usedPct;
    }

    public void setUsedPct(double usedPct) {
        this.usedPct = usedPct;
    }

    @Override
    public String toString(){
        return JsonUtil.toJson(this);
    }

    public static class QueueElement {

        private List<YarnQueue> queue;
        public List<YarnQueue> getQueue() {
            return queue;
        }

        public void setQueue(List<YarnQueue> queue) {
            this.queue = queue;
        }

        public static boolean isEmpty(QueueElement queueElement) {
            return queueElement == null ||
                    queueElement.getQueue() == null ||
                    queueElement.getQueue().size() == 0;
        }
    }
}
