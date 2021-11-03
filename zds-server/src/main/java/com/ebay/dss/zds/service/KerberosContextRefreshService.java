package com.ebay.dss.zds.service;

import com.ebay.dss.zds.kerberos.KerberosContext;
import com.ebay.dss.zds.kerberos.KerberosContextRefreshTask;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.concurrent.*;

@Service
public class KerberosContextRefreshService {

    private ConcurrentMap<KerberosContext, KerberosContextRefreshTask> taskMap = new ConcurrentHashMap<>();
    private int schedulerPoolSize = 2;
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(schedulerPoolSize);

    public boolean exist(KerberosContext context) {
        return taskMap.containsKey(context);
    }

    public void register(KerberosContext context) {
        taskMap.put(context, new KerberosContextRefreshTask(context, scheduler));
    }

    public void schedule(KerberosContext context) throws Exception {
        KerberosContextRefreshTask task = taskMap.get(context);
        task.call();
    }

    public void stop(KerberosContext context) {
        KerberosContextRefreshTask task = taskMap.remove(context);
        task.close();
    }

    @PreDestroy
    public void close() throws Exception {
        scheduler.shutdownNow();
        scheduler.awaitTermination(60, TimeUnit.SECONDS);
    }

}
