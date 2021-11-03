package com.ebay.dss.zds.message.queue;

import com.ebay.dss.zds.message.EventQueueIdentifier;
import com.ebay.dss.zds.message.ZetaEvent;
import com.ebay.dss.zds.message.ZetaEventListener;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static com.ebay.dss.zds.message.EventQueueIdentifier.OTHER;

/**
 * Created by tatian on 2019-06-10.
 */
@ThreadSafe
public class AsyncEventQueue implements EventQueue {

    protected static final Logger logger = LoggerFactory.getLogger(AsyncEventQueue.class);

    private String queueName;
    private AtomicBoolean started = new AtomicBoolean(false);
    private AtomicBoolean enabled = new AtomicBoolean(false);
    private AtomicLong handledEventCounter = new AtomicLong(0L);
    private AtomicLong droppedEventCounter = new AtomicLong(0L);
    public static final int defaultQueueSize = 10000;

    private static AtomicInteger nextQueueId = new AtomicInteger(0);

    private BlockingQueue<ZetaEvent> eventQueue;

    private List<Thread> dispatchThreads = new ArrayList<>();

    private List<ZetaEventListener> listeners = new CopyOnWriteArrayList<>();

    // this event is only for stop purpose
    private static final ZetaEvent stopEvent = new ZetaEvent() {
        @Override
        public JsonObject toJsonObject() { return null; }

        @Override
        public @NotNull EventQueueIdentifier getIdentifier() { return OTHER; }
    };

    public AsyncEventQueue(String queueName) {
        this(queueName, defaultQueueSize, 1);
    }

    public AsyncEventQueue(String queueName, int capacity) {
        this(queueName, capacity, 1);
    }

    public AsyncEventQueue(String queueName, int capacity, int dispatchThreadNum) {
       this(queueName, new LinkedBlockingQueue<>(capacity), dispatchThreadNum);
    }

    public AsyncEventQueue(String queueName, BlockingQueue<ZetaEvent> eventQueue, int dispatchThreadNum) {
        this.queueName = queueName;
        this.eventQueue = eventQueue;

        IntStream.range(0, dispatchThreadNum).forEach(i -> {
                    Thread thread = new Thread(() -> dispatch());
                    thread.setName("zeta-event-queue-thread-"
                            + queueName
                            + "-"
                            + getNextQueueId());
                    thread.setDaemon(true);
                    dispatchThreads.add(thread);
                }

        );
    }

    static Integer getNextQueueId() {
        return nextQueueId.getAndIncrement();
    }

    public AsyncEventQueue start() {
        synchronized (this) {
            if (!started.get()) {
                enable();
                this.started.set(true);
                this.dispatchThreads.forEach(Thread::start);
                logger.info("AsyncEventQueue: " + queueName + " started");
            } else logger.error("AsyncEventQueue: " + queueName + " already started");
            return this;
        }
    }

    public void enable() {
        this.enabled.set(true);
        logger.info("AsyncEventQueue: " + queueName + " enabled");
    }

    public void disable() {
        this.enabled.set(false);
        logger.info("AsyncEventQueue: " + queueName + " disabled");
    }

    public void stop() {
        synchronized (this) {
            if (started.get()) {
                disable();
                this.started.set(false);
                eventQueue.clear();
                droppedEventCounter.set(0L);
                dispatchThreads.forEach(t -> { if (t.isAlive()) eventQueue.offer(stopEvent); });
                logger.info("AsyncEventQueue: " + queueName + " stopped");
            } else logger.info("AsyncEventQueue: " + queueName + " already stopped");
        }
    }

    public void reset() {
        synchronized (this) {
            if (started.get()) {
                disable();
                eventQueue.clear();
                handledEventCounter.set(0L);
                droppedEventCounter.set(0L);
                enable();
                logger.info("AsyncEventQueue: " + queueName + " has reset");
            } else logger.info("AsyncEventQueue: " + queueName + " already stopped");
        }
    }

    private void dispatch() {
        try {
            while (this.started.get()) {
                // don't use poll, it cost a lot of cpu
                final ZetaEvent zetaEvent = eventQueue.take();
                if (zetaEvent != stopEvent) {
                    postToListeners(zetaEvent);
                    handledEventCounter.incrementAndGet();
                } else {
                    break;
                }
            }
            logger.info("Dispatcher thread: {} exited", Thread.currentThread().getName());
        } catch (Exception ex) {
            String threadName = Thread.currentThread().getName();
            this.stop();
            logger.error("Got exception in the event queue thread: " + threadName + " exit!");
        }
    }

    private void postToListeners(ZetaEvent zetaEvent) {
        listeners.forEach( listener -> {
            try {
                listener.onEventReceived(zetaEvent);
            } catch (Exception ex) {
                logger.error("Error when dispatch to listener: {}, ex: {}", listener.getClass().getName(), ex.toString());
            }
        });
    }

    public boolean post(@NotNull ZetaEvent zetaEvent) {
        if (!enabled.get()) {
            return false;
        }

        if (zetaEvent == null) {
            droppedEventCounter.incrementAndGet();
            return false;
        }

        if (eventQueue.offer(zetaEvent)) {
            return true;
        }

        droppedEventCounter.incrementAndGet();
        return false;
    }

    public String getQueueName() {
        return queueName;
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    public long getHandledEventCounter() {
        return this.handledEventCounter.get();
    }

    public long getDroppedEventCounter() {
        return this.droppedEventCounter.get();
    }

    public int getQueuedEventSize() { return this.eventQueue.size(); }

    public boolean addListener(ZetaEventListener listener) {
        if(this.listeners.add(listener)) {
            logger.info("New listener added in queue: " + queueName);
            return true;
        } else {
            logger.error("Failed to add listener to queue: " + queueName);
            return false;
        }
    }

    public void removeAllListener() {
        this.listeners.clear();
        logger.info("All listeners are cleared in queue: " + queueName);
    }

    public boolean removeListener(ZetaEventListener listener) {
        boolean removed = this.listeners.remove(listener);
        if (removed) {
            logger.info("Listener: " + listener.getClass().getSimpleName()
                + "is removed from queue: " + queueName);
        }
        return removed;
    }
}
