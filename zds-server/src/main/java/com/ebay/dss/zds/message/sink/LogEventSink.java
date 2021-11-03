package com.ebay.dss.zds.message.sink;

import com.ebay.dss.zds.message.queue.AsyncEventQueue;
import com.ebay.dss.zds.message.ZetaEvent;
import com.ebay.dss.zds.message.ZetaEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by tatian on 2019-06-11.
 */
public class LogEventSink implements ZetaEventListener {

    private static final Logger logger = LoggerFactory.getLogger(LogEventSink.class);

    private AsyncEventQueue listenedQueue;

    public LogEventSink(AsyncEventQueue listenedQueue) {
        this.listenedQueue = listenedQueue;
    }

    @Override
    public void onEventReceived(ZetaEvent zetaEvent) {
        logger.info(wrap(zetaEvent));
    }

    public String wrap(ZetaEvent zetaEvent) {
        return "QUEUE_" + listenedQueue.getQueueName()+ ": " + zetaEvent.toJson();
    }

}
