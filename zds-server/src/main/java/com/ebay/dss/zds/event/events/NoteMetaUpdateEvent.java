package com.ebay.dss.zds.event.events;

import org.springframework.context.ApplicationEvent;

public class NoteMetaUpdateEvent extends ApplicationEvent {

    private String id;
//    /** will only push to doe*/
//    private boolean doeOnly;
    private boolean buildRefs;

    public NoteMetaUpdateEvent(Object source, String id) {
        super(source);
        this.id = id;
        this.buildRefs = false;
    }

    public NoteMetaUpdateEvent(Object source, String id, boolean buildRefs) {
        super(source);
        this.id = id;
        this.buildRefs = buildRefs;
    }


    public String getId() {
        return id;
    }

    public boolean isBuildRefs() {
        return buildRefs;
    }
}
