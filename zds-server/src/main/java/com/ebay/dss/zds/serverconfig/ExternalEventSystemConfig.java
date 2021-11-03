package com.ebay.dss.zds.serverconfig;

import com.ebay.dss.zds.dao.ZetaEventRepository;
import com.ebay.dss.zds.service.MailService;
import com.ebay.dss.zds.service.MonitorService;
import com.ebay.dss.zds.message.queue.AsyncEventQueue;
import com.ebay.dss.zds.message.EventQueueIdentifier;
import com.ebay.dss.zds.message.ZetaEventBus;
import com.ebay.dss.zds.message.queue.HashAsyncEventQueueGroup.NTHashAsyncEventQueueGroup;
import com.ebay.dss.zds.message.sink.*;
import com.ebay.dss.zds.websocket.notebook.CodeController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Created by tatian on 2019-06-13.
 */
@Component
public class ExternalEventSystemConfig {

    private static final Logger logger = LogManager.getLogger();

    @Autowired
    MailService mailService;

    @Autowired
    ZetaEventRepository zetaEventRepository;

    @Autowired
    CodeController codeController;

    @Autowired
    MonitorService service;

    @Value("${zds.track.queue.group.monitor.queuenum}")
    private int monitorQueueGroupNum;

    public void init(ZetaEventBus zetaEventBus) {
        try {

            logger.info("Initializing external event system config...");

            // ---------------------operation queue---------------------
            // add queue && listener for operation event
            AsyncEventQueue operationQueue = new AsyncEventQueue(EventQueueIdentifier.OPERATION.name());
            operationQueue.addListener(new LogEventSink(operationQueue));
            // important: add an additional log file to record operation event
            operationQueue.addListener(new OperationLogEventSink(operationQueue));
            // save to db
            operationQueue.addListener(new MysqlEventSink(zetaEventRepository));
            zetaEventBus.addQueue(EventQueueIdentifier.OPERATION, operationQueue);

            /**
             * add your own message queue && listener here
             **/
            // ---------------------monitor queue---------------------
            NTHashAsyncEventQueueGroup ntHashAsyncEventQueueGroup =
                    new NTHashAsyncEventQueueGroup(monitorQueueGroupNum, EventQueueIdentifier.MONITOR.name());
            ntHashAsyncEventQueueGroup.addListener(new MonitorEventSink(service));
            ntHashAsyncEventQueueGroup.addListener(new EmailEventSink(mailService));
            zetaEventBus.addQueue(EventQueueIdentifier.MONITOR, ntHashAsyncEventQueueGroup);

            // ---------------------common queue---------------------
            AsyncEventQueue commonQueue = new AsyncEventQueue(EventQueueIdentifier.COMMON.name());
            commonQueue.addListener(new WebSocketEventSink(codeController));
            zetaEventBus.addQueue(EventQueueIdentifier.COMMON, commonQueue);

            // ---------------------metric queue---------------------
            AsyncEventQueue metricQueue = new AsyncEventQueue(EventQueueIdentifier.METRIC.name());
            metricQueue.addListener(new MetricReportSink());
            zetaEventBus.addQueue(EventQueueIdentifier.METRIC, metricQueue);

            /**
             * add your own message queue && listener above
             **/

            logger.info("External event system config inited");

        } catch (Exception ex) {
            logger.error("Failed to initialize external event system config");
        }

    }
}
