package com.ebay.dss.zds.interpreter.monitor.common;

import com.ebay.dss.zds.interpreter.ConfigurationManager.Cluster;
import com.ebay.dss.zds.interpreter.monitor.modle.YarnQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by tatian on 2019-04-19.
 */
public class ClusterStatusDengineRESTImpl implements ClusterStatus {

    //https://hercules-lvs-rm-2.vip.ebay.com:50030/ws/v1/cluster/scheduler
    protected static final Logger logger = LoggerFactory.getLogger(ClusterStatusYarnRESTImpl.class);
    private Cluster cluster;
    private String baseUrl;
    private YarnStatusHTTPClient client;
    private final String schedulerPath;

    public ClusterStatusDengineRESTImpl(String baseUrl, Cluster cluster) {
        this.cluster = cluster;
        this.baseUrl = baseUrl;
        this.client = YarnStatusHTTPClient.create();
        this.schedulerPath = baseUrl + "/yarn?clusterId=" + cluster.getId();
    }

    public List<YarnQueue> getYarnQueues() {
        return client.getQueues(schedulerPath);
    }

    public YarnQueue getYarnQueue(String queueName) {

        LinkedList<YarnQueue> queues = new LinkedList<>(Optional
                .ofNullable(getYarnQueues())
                .orElse(new ArrayList<>()));
        YarnQueue targetQueue = null;
        while (!queues.isEmpty()) {
            YarnQueue head = queues.pollFirst();
            if (head != null) {
                if (queueName.equals(head.getQueueName())) {
                    targetQueue = head;
                    break;
                } else if (!YarnQueue.QueueElement.isEmpty(head.getQueues())) {
                    for(YarnQueue e : head.getQueues().getQueue()) {
                        queues.addLast(e);
                    }
                }
            }
        }
        return targetQueue;
    }

}
