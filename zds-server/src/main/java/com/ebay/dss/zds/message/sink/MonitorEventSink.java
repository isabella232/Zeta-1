package com.ebay.dss.zds.message.sink;

import com.ebay.dss.zds.service.MonitorService;
import com.ebay.dss.zds.message.ZetaEvent;
import com.ebay.dss.zds.message.ZetaEventListener;
import com.ebay.dss.zds.message.event.ZetaMonitorEvent;

/**
 * Created by tatian on 2019-07-20.
 */
public class MonitorEventSink implements ZetaEventListener {

    private MonitorService monitorService;

    public MonitorEventSink(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @Override
    public void onEventReceived(ZetaEvent zetaEvent) {
        if (zetaEvent instanceof ZetaMonitorEvent) {
            monitorService.handleMonitorEvent((ZetaMonitorEvent)zetaEvent);
        }
    }
}
