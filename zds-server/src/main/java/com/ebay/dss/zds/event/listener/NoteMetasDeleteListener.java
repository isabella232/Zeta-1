package com.ebay.dss.zds.event.listener;

import com.ebay.dss.zds.event.events.NoteMetasDeleteEvent;
import com.ebay.dss.zds.service.DoeESService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class NoteMetasDeleteListener implements ApplicationListener<NoteMetasDeleteEvent> {

    @Autowired
    DoeESService doeESService;

    @Async("asyncEventExecutor")
    @Override
    public void onApplicationEvent(NoteMetasDeleteEvent event) {
        doeESService.batchDeleteDOEMeta(event.getIds());
    }
}
