package com.ebay.dss.zds.event.events;

import org.springframework.context.ApplicationEvent;

public class NoteMetaDeleteEvent extends ApplicationEvent {

    private String id;
    /** will only push to doe*/
    private boolean doeOnly;

    public NoteMetaDeleteEvent(Object source, String id) {
        super(source);
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public boolean isDoeOnly() {
        return doeOnly;
    }
}
