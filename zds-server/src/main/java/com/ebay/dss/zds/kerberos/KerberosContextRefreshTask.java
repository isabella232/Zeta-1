package com.ebay.dss.zds.kerberos;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;

public class KerberosContextRefreshTask implements Callable<Boolean> {

    private volatile boolean closed = false;
    private KerberosContext context;
    private ScheduledExecutorService scheduler;

    public KerberosContextRefreshTask(KerberosContext context,
                               ScheduledExecutorService scheduler) {
        this.context = context;
        this.scheduler = scheduler;
    }

    public boolean isClosed() {
        return closed;
    }

    public void close() {
        closed = true;
    }

    @Override
    public Boolean call() throws Exception {
        context.doKerberosLogin();
        if (isClosed() || Objects.isNull(context) || context.isClosed()) {
            return false;
        }
        scheduler.schedule(this,
                context.getRefreshIntervalScale(),
                context.getRefreshIntervalUnit()
        );
        return true;
    }
}
