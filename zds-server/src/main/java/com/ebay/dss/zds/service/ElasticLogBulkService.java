package com.ebay.dss.zds.service;

import com.ebay.dss.zds.serverconfig.ElasticConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.core.TimeValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

@Service
@ConditionalOnProperty(value = "zds.elastic.log.enabled")
public class ElasticLogBulkService implements ElasticLogService {

    private static final Logger logger = LogManager.getLogger();
    private final RestHighLevelClient client;
    private final ElasticConfig.ElasticLogProperties properties;

    private ConcurrentLinkedQueue<Indexable> queue = new ConcurrentLinkedQueue<>();
    private AtomicLong lastBulkTime = new AtomicLong(System.currentTimeMillis());

    @Autowired
    public ElasticLogBulkService(RestHighLevelClient client,
                                 ElasticConfig.ElasticLogProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public boolean add(Indexable indexable) {
        if (properties.getEnabled()) {
            return queue.add(indexable);
        } else {
            return false;
        }
    }

    @Override
    @Async("ElasticLogTaskExecutor")
    public synchronized void index() {
        if (!properties.getEnabled()) return;

        if (queue.size() <= 0) {
            logger.debug("Queue is empty, skip...");
            return;
        }
        logger.debug("Queue size: {}", queue.size());
        long resultSize = bulkIndexUntil(properties.getBulkSize());
        if (resultSize <= 0) {
            resultSize = idleTimeoutBulkIndex();
        }
        if (resultSize > 0) {
            lastBulkTime.getAndSet(System.currentTimeMillis());
            logger.debug("Push {} records successfully", resultSize);
        }
    }

    @PreDestroy
    public void close() {
        logger.info("Shutdown logging, bulk all remaining events...");
        if (!properties.getEnabled()) return;
        bulkIndexUntil(0);
        logger.info("Shutdown logging done...");
    }

    private Long bulkIndexUntil(long bulkThreshold) {
        Long total = 0L;
        while (queue.size() > bulkThreshold) {
            try {
                total += bulkIndex();
            } catch (Exception e) {
                logger.error("Bulk indexJob error: ", e);
            }
        }
        return total;
    }

    private Long bulkIndex() throws IOException {
        List<Indexable> aceSearchLogs = new ArrayList<>();
        while (aceSearchLogs.size() < properties.getBulkSize() && queue.size() > 0) {
            aceSearchLogs.add(queue.poll());
        }

        BulkRequest request = buildBulkRequest(aceSearchLogs);
        BulkResponse response = client.bulk(request, RequestOptions.DEFAULT);
        return errorAndGetResultSizeOf(response);
    }

    private BulkRequest buildBulkRequest(Collection<Indexable> logs) {
        IndexRequest[] indexRequests = logs.stream()
                .filter(Objects::nonNull)
                .map(Indexable::toIndexRequest)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toArray(IndexRequest[]::new);
        BulkRequest bulkRequest = new BulkRequest();
        bulkRequest.timeout(TimeValue.timeValueMillis(properties.getBulkTimeout()));
        bulkRequest.add(indexRequests);
        return bulkRequest;

    }

    private Long errorAndGetResultSizeOf(BulkResponse response) {
        if (response.hasFailures()) {
            logger.warn("Some of indexJob request error: {}", response.buildFailureMessage());
        }
        long success = 0;
        for (BulkItemResponse itemResponse : response) {
            success += itemResponse.isFailed() ? 0 : 1;
        }
        return success;
    }

    private long idleTimeoutBulkIndex() {
        if (System.currentTimeMillis() - lastBulkTime.get() < properties.getBulkIdleTimeout()) {
            return 0;
        }
        logger.debug("Service timeout in idle, pushing all objects in queue");
        return bulkIndexUntil(0);
    }

}
