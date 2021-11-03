package com.ebay.dss.zds.interpreter.monitor.common;

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


public class ClusterStatusYarnRESTImpl implements ClusterStatus {

    //https://hercules-lvs-rm-2.vip.ebay.com:50030/ws/v1/cluster/scheduler
    protected static final Logger logger = LoggerFactory.getLogger(ClusterStatusYarnRESTImpl.class);
    private static final String schedulerUrlTail="/ws/v1/cluster/scheduler";
    private YarnUrl baseUrl;
    private YarnStatusHTTPClient client;

    public ClusterStatusYarnRESTImpl(String baseUrl) {
        YarnUrl url=new YarnUrl(baseUrl,YarnUrl.findBackupUrl(baseUrl));
        init(url);
    }

    public ClusterStatusYarnRESTImpl(YarnUrl baseUrl) {
        init(baseUrl);
    }

    public ClusterStatusYarnRESTImpl init(YarnUrl yarnUrl) {
        this.baseUrl = yarnUrl;
        this.client = YarnStatusHTTPClient.create();
        return this;
    }

    public static ClusterStatusYarnRESTImpl fromUrl(String url) {
        Matcher matcher = Pattern.compile("https{0,1}.*?:50030")
                .matcher(url);
        if (matcher.find()) {
            return new ClusterStatusYarnRESTImpl(matcher.group());
        } else {
            throw new RuntimeException("Failed to match cluster yarn url");
        }
    }

    public List<YarnQueue> getYarnQueues() {
        return Optional.ofNullable(client.getQueues(baseUrl.getUrl()+schedulerUrlTail))
                .orElse(client.getQueues(baseUrl.getUrl_backup()+schedulerUrlTail));
    }

    /*
    public YarnQueue getYarnQueue(String queueName) {
        List<YarnQueue> queues=Optional.ofNullable(getYarnQueues()).orElse(new ArrayList<>())
                .stream()
                .filter(queue -> queueName.equals(queue.getQueueName()))
                .collect(Collectors.toList());
        return queues.size()>0?queues.get(0):null;
    }*/

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

    public YarnUrl getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(YarnUrl baseUrl) {
        this.baseUrl = baseUrl;
    }
}
