package com.ebay.dss.zds.message.queue;

import com.ebay.dss.zds.message.TaggedEvent;
import com.ebay.dss.zds.message.ZetaEvent;

import javax.annotation.concurrent.ThreadSafe;
import javax.validation.constraints.NotNull;

/**
 * Created by tatian on 2019-06-21.
 */
@ThreadSafe
public class HashAsyncEventQueueGroup extends AbstractAsyncEventQueueGroup{

    public HashAsyncEventQueueGroup() {}

    public HashAsyncEventQueueGroup(int queueNum, String name) {
        super(queueNum, name);
    }

    protected int getKey(@NotNull ZetaEvent zetaEvent) {
        if (zetaEvent == null) throw new UnsupportedOperationException("The event shouldn't be null");
        if (!started) throw new UnsupportedOperationException("The " + this.getClass().getName()
                + " is not started");
        int hash = zetaEvent.hashCode();
        return Math.abs(hash) % group.size();
    }

    public static class NTHashAsyncEventQueueGroup extends HashAsyncEventQueueGroup {

        public NTHashAsyncEventQueueGroup() {}

        public NTHashAsyncEventQueueGroup(int queueNum, String name) {
            super(queueNum, name);
        }

        @Override
        protected int getKey(@NotNull ZetaEvent zetaEvent) {
            if (zetaEvent == null) throw new UnsupportedOperationException("The event shouldn't be null");
            if (zetaEvent instanceof TaggedEvent.NTTagged) {
                if (!started) throw new UnsupportedOperationException("The " + this.getClass().getName()
                        + " is not started");
                int hash = ((TaggedEvent.NTTagged)zetaEvent).getNt().hashCode();
                return Math.abs(hash) % group.size();
            } else return super.getKey(zetaEvent);
        }
    }

}
