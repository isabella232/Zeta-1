package com.ebay.dss.zds.message.sink;

import com.ebay.dss.zds.dao.ZetaEventRepository;
import com.ebay.dss.zds.message.TaggedEvent;
import com.ebay.dss.zds.message.ZetaEvent;
import com.ebay.dss.zds.message.ZetaEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by tatian on 2019-06-19.
 */
public class MysqlEventSink implements ZetaEventListener {

    private static final Logger logger = LoggerFactory.getLogger(MysqlEventSink.class);

    ZetaEventRepository zetaEventRepository;

    public MysqlEventSink(ZetaEventRepository zetaEventRepository) {
        this.zetaEventRepository = zetaEventRepository;
    }

    @Override
    public void onEventReceived(ZetaEvent zetaEvent) {
        if (zetaEvent instanceof TaggedEvent.MysqlStorable) {
            String eventJson = zetaEvent.toJson();
            long r;
            try {
                r = ((TaggedEvent.MysqlStorable) zetaEvent).store(zetaEventRepository);
            } catch (Exception ex) {
                r = -1;
                logger.info("Failed to save to db: {}, ex: {}", eventJson, ex.toString());
            }
            if (r > 0) {
                logger.info("Saved to db: " + eventJson);
            } else {
                logger.info("Failed to save to db: " + eventJson);
            }
        } else logger.info("The event is not an mysql storable operation event, skip this one: " + zetaEvent.getClass().getName());
    }
}
