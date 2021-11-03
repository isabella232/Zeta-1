package com.ebay.dss.zds.message;

import com.ebay.dss.zds.serverconfig.ExternalEventSystemConfig;
import com.ebay.dss.zds.message.queue.AsyncEventQueue;
import com.ebay.dss.zds.message.queue.EventQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by tatian on 2019-06-10.
 */
@Component
public class ZetaEventBus {

    protected static final Logger logger = LoggerFactory.getLogger(ZetaEventBus.class);

    private ConcurrentHashMap<EventQueueIdentifier, EventQueue> queues = new ConcurrentHashMap<>();

    @Value("${zds.track.event.queue.maxAutoExpandThreshold}")
    private int maxAutoExpandThreshold;

    private ExternalEventSystemConfig externalEventSystemConfig;

    private static ZetaEventBus _bus;

    @Autowired
    public ZetaEventBus(ExternalEventSystemConfig externalEventSystemConfig) {
        this.externalEventSystemConfig = externalEventSystemConfig;
    }

    @PostConstruct
    public void init() {

        if (maxAutoExpandThreshold == 0) {
            maxAutoExpandThreshold = EventQueueIdentifier.values().length;
            logger.warn("The maxAutoExpandThreshold is set to 0, set to " + maxAutoExpandThreshold + " now");
        }

        logger.info("Initializing ZetaEventBus...");

        externalEventSystemConfig.init(this);

        _bus = this;

        EventTracker.setZetaEventBus(this);

        logger.info("ZetaEventBus initialized");
    }

    public synchronized EventQueue addQueue(EventQueueIdentifier identifier, EventQueue queue) {
        if (queues.containsKey(identifier)) {
            logger.error("Failed to add queue: " + identifier + " the queue already exist!");
            return queues.get(identifier);
        }
        return queues.put(identifier, queue.start());
    }

    public Optional<EventQueue> findQueue(EventQueueIdentifier identifierer) {
        return Optional.of(queues.get(identifierer));
    }

    public synchronized AsyncEventQueue createNewQueue(EventQueueIdentifier identifier) {
        AsyncEventQueue newQueue = new AsyncEventQueue(identifier.name()).start();
        addQueue(identifier, newQueue);
        return newQueue;
    }

    public synchronized EventQueue removeQueue(EventQueueIdentifier identifier) {
        EventQueue queue = queues.remove(identifier);
        if (queue != null) queue.stop();
        return queue;
    }

    public synchronized boolean addListener(EventQueueIdentifier identifier, ZetaEventListener listener) {
        if (queues.containsKey(identifier)) {
            boolean added = queues.get(identifier).addListener(listener);
            if (added) {
                logger.info("Successed to add listener to queue: " + identifier);
            } else logger.error("Failed to add listener to queue: " + identifier);
            return added;
        } else {
            if (queues.size() < maxAutoExpandThreshold) {
                AsyncEventQueue newQueue = new AsyncEventQueue(identifier.name());
                boolean added = newQueue.addListener(listener);
                if (added) {
                    queues.put(identifier, newQueue.start());
                    logger.info("New queue added: " + identifier + " (" + queues.size() + "/" + maxAutoExpandThreshold + ")");
                    logger.info("Successed to add listener to queue: " + identifier);
                } else logger.error("Failed to add listener to queue: " + identifier);
                return added;
            } else {
                logger.error("Failed to add listener to queue: " + identifier
                        + " where is no such queue and no space for auto expand ");
                return false;
            }
        }
    }

    public synchronized void removeAllListener(EventQueueIdentifier identifier) {
        EventQueue eventQueue = queues.get(identifier);
        if (eventQueue != null) eventQueue.removeAllListener();
    }

    public synchronized boolean removeListener(EventQueueIdentifier identifier, ZetaEventListener listener) {
        EventQueue eventQueue = queues.get(identifier);
        if (eventQueue != null) {
            return eventQueue.removeListener(listener);
        } else return true;
    }

    public boolean postToQueue(EventQueueIdentifier identifier, ZetaEvent zetaEvent) {
        EventQueue eventQueue = queues.get(identifier);
        if (eventQueue != null) {
            return eventQueue.post(zetaEvent);
        } else {
            synchronized(this) {
                if (queues.size() < maxAutoExpandThreshold) {
                    AsyncEventQueue newQueue = createNewQueue(identifier);
                    logger.info("New queue added: " + identifier + " (" + queues.size() + "/" + maxAutoExpandThreshold + ")");
                    return newQueue.post(zetaEvent);
                } else {
                    logger.error("There is no queue: " + identifier + " and no space for auto expand ");
                    return false;
                }
            }
        }
    }

    public boolean postToQueue(ZetaEvent zetaEvent) {
        EventQueueIdentifier identifier = zetaEvent.getIdentifier();
        return postToQueue(identifier, zetaEvent);
    }

    public int getQueueSize() {
        return this.queues.size();
    }

    protected void cleanUp() {
        queues.values().forEach(EventQueue::stop);
        queues.clear();
    }

    protected static Optional<ZetaEventBus> getInstance() {
        return Optional.of(_bus);
    }

}
