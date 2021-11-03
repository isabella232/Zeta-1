package com.ebay.dss.zds.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Created by tatian on 2019-06-10.
 */
@ThreadSafe
public class EventTracker {

    protected static final Logger logger = LoggerFactory.getLogger(EventTracker.class);

    private static volatile ZetaEventBus zetaEventBus;

    protected static void setZetaEventBus(ZetaEventBus zetaEventBus) {
        EventTracker.zetaEventBus = zetaEventBus;
    }

    @SuppressWarnings("Do not use this in static code block")
    public static boolean postEvent(ZetaEvent zetaEvent) {
        return postEvent(zetaEvent.getIdentifier(), zetaEvent);
    }

    @SuppressWarnings("Do not use this in static code block")
    public static boolean postEvent(EventQueueIdentifier identifier, ZetaEvent zetaEvent) {
        if (zetaEventBus == null) {
            logger.info("The ZetaEventBus is not injected yet, skip this event: {}", zetaEvent.toJson());
            return false;
        }
        boolean success;
        try {
            success = zetaEventBus.postToQueue(identifier, zetaEvent);
        } catch (Exception ex) {
            logger.error(ex.toString());
            success = false;
        }
        return success;
    }

}
