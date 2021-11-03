package com.ebay.dss.zds.message.queue;

import com.ebay.dss.zds.message.ZetaEvent;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Created by tatian on 2019-07-04.
 */
@ThreadSafe
public class AsyncPriorityEventQueue extends AsyncEventQueue{

    public AsyncPriorityEventQueue(String queueName, Comparator<ZetaEvent> comparator) {
        super(queueName, new PriorityBlockingQueue<>(10000, comparator), 1);
    }

    public AsyncPriorityEventQueue(String queueName, int capacity, Comparator<ZetaEvent> comparator, int dispatchThreadNum) {
        super(queueName, new PriorityBlockingQueue<>(capacity, comparator), dispatchThreadNum);
    }

}
