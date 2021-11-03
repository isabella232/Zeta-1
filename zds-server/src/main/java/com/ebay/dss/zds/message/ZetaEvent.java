package com.ebay.dss.zds.message;

import com.google.gson.JsonObject;
import org.joda.time.DateTime;

import javax.validation.constraints.NotNull;
import java.util.Map;
import java.util.Properties;

/**
 * Created by tatian on 2019-06-10.
 */
public abstract class ZetaEvent implements Runnable {

    private Properties externalContext = new Properties();

    protected final DateTime recordTime = new DateTime();

    public String toJson() {
        return toJsonObject().toString();
    }

    public abstract JsonObject toJsonObject();

    @NotNull
    public abstract EventQueueIdentifier getIdentifier();

    public Properties getExternalContext() {
        return externalContext;
    }

    public void setExternalContext(Properties externalContext) {
        this.externalContext = externalContext;
    }

    public ZetaEvent addProperty(String key, String value) {
        this.externalContext.setProperty(key, value);
        return this;
    }

    public ZetaEvent addProperties(Map<String, String> prop) {
        this.externalContext.putAll(prop);
        return this;
    }

    public ZetaEvent addProperties(Properties prop) {
        this.externalContext.putAll(prop);
        return this;
    }

    public DateTime getRecordTime() {
        return recordTime;
    }

    @Override
    public void run() {
        // do nothing by default
    }
}
