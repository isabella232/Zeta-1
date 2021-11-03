package com.ebay.dss.zds.message.sink;

import com.ebay.dss.zds.message.TaggedEvent;
import com.ebay.dss.zds.message.ZetaEvent;
import com.ebay.dss.zds.message.ZetaEventListener;
import com.ebay.dss.zds.message.event.TagMetricEvent;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Point;

import javax.annotation.PreDestroy;

public class InfluxEventSink implements ZetaEventListener {

    private static final Logger logger = LogManager.getLogger();

    private InfluxDB db;
    private Boolean enabled;

    public Boolean getEnabled() {
        return enabled;
    }

    public InfluxEventSink setEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public InfluxEventSink(InfluxDB db) {
        this.db = db;
        this.enabled = true;
    }

    @Override
    public void onEventReceived(ZetaEvent zetaEvent) {
        if (zetaEvent instanceof TaggedEvent.InfluxStorable) {
            if (zetaEvent instanceof TagMetricEvent) {
                Point point = Point.measurement(((TaggedEvent.InfluxStorable) zetaEvent).measurement())
                        .tag(((TagMetricEvent) zetaEvent).getTags())
                        .fields(((TagMetricEvent) zetaEvent).getMetrics())
                        .build();
                db.write(point);
            }
        }
    }

    @PreDestroy
    public void close() {
        logger.info("shutdown influx event sink...");
        db.flush();
        db.close();
        logger.info("shutdown influx event sink, done");
    }
}
