package com.ebay.dss.zds.message;

/**
 * Created by tatian on 2019-06-10.
 */
public enum EventQueueIdentifier {

    COMMON("common"),
    OPERATION("operation"),
    EXCEPTION("exception"),
    LIFECYCLE("lifecycle"),
    MONITOR("monitor"),
    METRIC("metric"),
    STATE("state"),
    OTHER("other");

    // todo: add more queue identifier

    private String identifier;

    EventQueueIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return this.identifier;
    }

}
