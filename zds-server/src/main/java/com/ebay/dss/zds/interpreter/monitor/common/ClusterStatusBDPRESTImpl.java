package com.ebay.dss.zds.interpreter.monitor.common;

import com.ebay.dss.zds.interpreter.ConfigurationManager;
import com.ebay.dss.zds.interpreter.monitor.modle.YarnQueue;
import org.apache.commons.collections.map.SingletonMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by tatian on 2019-02-26.
 */
public class ClusterStatusBDPRESTImpl implements ClusterStatus {

    protected static final Logger logger = LoggerFactory.getLogger(ClusterStatusBDPRESTImpl.class);
    private BDPHTTPClient client;

    public ClusterStatusBDPRESTImpl(ConfigurationManager.Cluster cluster){
        this.client = new BDPHTTPClient(cluster);
    }

    public ClusterStatusBDPRESTImpl(BDPHTTPClient client) {
        this.client = client;
    }

    public List<YarnQueue> getYarnQueues() {
        return client.getQueues();
    }

    public YarnQueue getYarnQueue(String queueName) {
        return client.getQueue(queueName);
    }
}
