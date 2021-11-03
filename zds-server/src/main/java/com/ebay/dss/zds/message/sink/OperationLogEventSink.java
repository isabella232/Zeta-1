package com.ebay.dss.zds.message.sink;

import com.ebay.dss.zds.message.queue.AsyncEventQueue;
import com.ebay.dss.zds.message.ZetaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by tatian on 2019-06-17.
 *
 * This sink using a different log file to track the log operation events
 */
public class OperationLogEventSink extends LogEventSink {

    private static final Logger logger = LoggerFactory.getLogger(OperationLogEventSink.class);

    private AsyncEventQueue listenedQueue;

    public OperationLogEventSink(AsyncEventQueue listenedQueue) {
        super(listenedQueue);
        this.listenedQueue = listenedQueue;
    }

    @Override
    public void onEventReceived(ZetaEvent zetaEvent) {
        logger.info(wrap(zetaEvent));
    }
}
