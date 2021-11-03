package com.ebay.dss.zds.message.queue;

import com.ebay.dss.zds.message.ZetaEvent;
import com.ebay.dss.zds.message.ZetaEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.IntStream;

/**
 * Created by tatian on 2019-06-21.
 */
public abstract class AbstractAsyncEventQueueGroup implements EventQueue {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractAsyncEventQueueGroup.class);
    
    protected List<AsyncEventQueue> group = new CopyOnWriteArrayList<>();
    protected volatile boolean started = false;

    public AbstractAsyncEventQueueGroup() {}

    public AbstractAsyncEventQueueGroup(int queueNum, String name) {
        IntStream.rangeClosed(1, queueNum).forEach(i -> addQueue(new AsyncEventQueue(name + "_" + i)));
    }

    public boolean post(@NotNull ZetaEvent zetaEvent) {
        if (group.size() == 0) return false;
        try {
            AsyncEventQueue queue = group.get(getKey(zetaEvent));
            return queue.post(zetaEvent);
        } catch (Exception ex) {
            logger.error(ex.toString());
            return false;
        }
    }

    // This method should guarantee thread-safe
    protected abstract int getKey(@NotNull ZetaEvent zetaEvent);

    public boolean addQueue(AsyncEventQueue queue) {
        if (started) return false;
        boolean added = this.group.add(queue);
        logger.info("AsyncEventQueue added: " + queue.getQueueName());
        return added;
    }

    public EventQueue start() {
        if (group.size() == 0)
            throw new UnsupportedOperationException("The " + this.getClass().getName()
                    + " can't start with 0 queue in it");
        group.forEach(AsyncEventQueue::start);
        started = true;
        logger.info("All AsyncEventQueue started: {}", group.size());
        return this;
    }

    public void stop() {
        group.forEach(AsyncEventQueue::stop);
        started = false;
        logger.info("All AsyncEventQueue stopped: {}", group.size());
    }

    public boolean addListener(ZetaEventListener listener) {
        boolean allAdded = group
                .stream()
                .map(queue -> queue.addListener(listener))
                .filter(added -> added)
                .count() == group.size();
        if (allAdded) {
            logger.info("Listener is all added");
            return true;
        } else {
            logger.info("Failed to add listeners");
            removeAllListener();
            return false;
        }
    }

    public void removeAllListener() {
        group.forEach(AsyncEventQueue::removeAllListener);
        logger.info("All listeners are cleared in queue group");
    }

    public boolean removeListener(ZetaEventListener listener) {
        boolean allRemoved = group
                .stream()
                .map(queue -> queue.removeListener(listener))
                .filter(added -> added)
                .count() == group.size();
        if (allRemoved) {
            logger.info("Listener is all removed");
            return true;
        } else {
            logger.info("Failed to remove all listeners");
            return false;
        }
    }

}
