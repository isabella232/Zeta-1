package com.ebay.dss.zds.interpreter.monitor.common;

import com.ebay.dss.zds.interpreter.monitor.modle.YarnQueue;

import java.util.List;
import java.util.Properties;

/**
 * Created by tatian on 2019-02-26.
 */
public interface ClusterStatus {

    public YarnQueue getYarnQueue(String key);
    public List<YarnQueue> getYarnQueues();

}
