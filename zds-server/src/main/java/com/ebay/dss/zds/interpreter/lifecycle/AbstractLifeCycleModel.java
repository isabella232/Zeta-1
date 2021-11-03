package com.ebay.dss.zds.interpreter.lifecycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Created by tatian on 2018/5/29.
 */
abstract class AbstractLifeCycleModel extends LifeCycleModel {

    protected LifeCycleManager manager;
    protected Properties prop;

    protected AbstractLifeCycleModel(LifeCycleManager manager) {
        this.manager = manager;
        this.prop = new Properties();
    }

    protected AbstractLifeCycleModel(LifeCycleManager manager, Properties prop) {
        this.manager = manager;
        this.prop = prop;
    }

}
