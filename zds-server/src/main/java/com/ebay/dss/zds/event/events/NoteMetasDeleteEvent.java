package com.ebay.dss.zds.event.events;

import org.springframework.context.ApplicationEvent;

import java.util.List;

public class NoteMetasDeleteEvent extends ApplicationEvent {

    private List<String> ids;
    public NoteMetasDeleteEvent(Object source, List<String> ids) {
        super(source);
        this.ids = ids;
    }

    public List<String> getIds() {
        return ids;
    }
}
