package com.ebay.dss.zds.message.queue;

import com.ebay.dss.zds.message.ZetaEvent;

import javax.annotation.concurrent.ThreadSafe;
import javax.validation.constraints.NotNull;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by tatian on 2019-06-21.
 */
@ThreadSafe
public class RoundRobinAsyncEventQueueGroup extends AbstractAsyncEventQueueGroup {

    private AtomicBoolean mark = new AtomicBoolean(true);

    private AtomicInteger key = new AtomicInteger();

    public RoundRobinAsyncEventQueueGroup() {}

    public RoundRobinAsyncEventQueueGroup(int queueNum, String name) {
        super(queueNum, name);
    }

    protected int getKey(@NotNull ZetaEvent zetaEvent) {
        if (!started) throw new UnsupportedOperationException("The " + this.getClass().getName()
                + " is not started");
        for(;;) {
            if(mark.compareAndSet(true, false)) {
                int current = key.get();
                if(key.incrementAndGet() == group.size()) {
                    key.set(0);
                }
                mark.set(true);
                return current;
            }
        }
    }
}
