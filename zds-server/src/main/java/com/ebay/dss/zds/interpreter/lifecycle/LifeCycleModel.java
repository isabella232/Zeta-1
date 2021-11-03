package com.ebay.dss.zds.interpreter.lifecycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by tatian on 2018/5/28.
 */
@NotThreadSafe
public abstract class LifeCycleModel {

    protected static final Logger logger = LoggerFactory.getLogger(LifeCycleModel.class);

    private AtomicBoolean isEnded = new AtomicBoolean(false);

    public abstract LifeCycleModel create();

    public abstract LifeCycleModel bind(Object obj);

    public abstract Object getCompanionObject();

    public abstract boolean isAlive();

    public abstract void keepAlive();

    public abstract void end();

    public abstract String endReason();

    public void tryEnd() {
        if (!isEnded.getAndSet(true)) {
            end();
        } else logger.error("The lifecycle already ended once!");
    }

    public abstract HealthReport getHealthReport();
}
