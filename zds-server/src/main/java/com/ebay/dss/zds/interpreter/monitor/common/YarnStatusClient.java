package com.ebay.dss.zds.interpreter.monitor.common;

import com.ebay.dss.zds.interpreter.monitor.modle.YarnQueue;

import java.util.List;

/**
 * Created by tatian on 2019-02-25.
 */
public interface YarnStatusClient {

    YarnQueue getQueue(String name);
    // todo: maybe need to add more interface, no other requirement yet
}
