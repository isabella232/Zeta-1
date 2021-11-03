package com.ebay.dss.zds.message.endpoint;

import com.ebay.dss.zds.common.NetworkUtils;
import com.ebay.dss.zds.interpreter.ConfigurationManager.Cluster;
import com.ebay.dss.zds.serverconfig.ServerConfigurationContext;
import io.micrometer.core.instrument.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;

/**
 * Created by tatian on 2019-06-20.
 */

@Component("prometheusMetricRegistry")
public class PrometheusMetricRegistry {

    private static final Logger logger = LoggerFactory.getLogger(PrometheusMetricRegistry.class);

    @Autowired
    InterpreterActuatorEndpoint endpoint;

    @Autowired
    BDPServiceEndpoint bdpServiceEndpoint;

    @Autowired
    LivyMonitorEndpoint livyMonitorEndpoint;

    private static String serverName;

    private static ArrayList<Tag> serverList;

    private static String getService() {
        if (serverName == null) serverName = NetworkUtils.getHostName();
        return serverName;
    }

    private static List<Tag> init(){
        if (serverList == null) {
            serverList = new ArrayList<>();
            serverList.add(new ImmutableTag("service", getService()));
        }
        return serverList;
    }

    /**
     * name: metric name in prometheus
     * obj: the object that will execute the method that generate metric value
     * valueFunction: the function that generate the metric value by obj
     * */
    private static <T> T registerMetric(String name, T obj, ToDoubleFunction<T> valueFunction) {
        Metrics.gauge(name, init(),
                obj, valueFunction);
        logger.info("Registered prometheus value: {}", name);
        return obj;
    }

    private void registerInterpreterMetrics(String tag) {
        String name = "interpreter." + tag + ".guage.value";
        registerMetric(name, endpoint, endpoint -> endpoint.getInterpreterStatusCount(tag));
    }

    private void registerConnectionQueueMetrics(Cluster cluster) {
        String name = "connection.yarn.queue." + cluster.name() + ".guage.value";
        registerMetric(name, bdpServiceEndpoint, endpoint -> endpoint.getConnectionQueueUsedPct(cluster.name()));
    }

    private void registerThreadPoolMetrics(ThreadPoolTaskExecutor threadPoolExecutor, String executorName) {
        Iterable<Tag> tagsWithId = Tags.concat(init(), "executor_name", executorName);

        Gauge.builder("zeta.spring.executor.pool.size",
                threadPoolExecutor, ThreadPoolTaskExecutor::getPoolSize)
                .tags(tagsWithId)
                .description("The executor's current pool size")
                .register(Metrics.globalRegistry);

        Gauge.builder("zeta.spring.executor.active.threads.count",
                threadPoolExecutor, ThreadPoolTaskExecutor::getActiveCount)
                .tags(tagsWithId)
                .description("The executor's current active threads count")
                .register(Metrics.globalRegistry);

        Gauge.builder("zeta.spring.executor.queued.tasks.count",
                threadPoolExecutor, executor -> executor.getThreadPoolExecutor().getQueue().size())
                .tags(tagsWithId)
                .description("The executor's queued tasks count")
                .register(Metrics.globalRegistry);

        Gauge.builder("zeta.spring.executor.completed.tasks.count",
                threadPoolExecutor, executor -> executor.getThreadPoolExecutor().getCompletedTaskCount())
                .tags(tagsWithId)
                .description("The executor's completed count")
                .register(Metrics.globalRegistry);
    }

    private void registerLivyStatementMetrics() {
        registerMetric("zeta.statement.longrunning.count", endpoint,
                InterpreterActuatorEndpoint::getLongRunningCount);
        registerMetric("zeta.statement.bigcputtime.count", endpoint,
                InterpreterActuatorEndpoint::getBigCPUTimeCount);
        registerMetric("zeta.statement.bigmemoryseconds.count", endpoint,
                InterpreterActuatorEndpoint::getBigMemorySecondsCount);
    }

    private void registerLivyInstanceMonitorMetrics(Cluster cluster) {
        Iterable<Tag> tagsWithId = Tags.concat(init(), "cluster_id", cluster.getId() + "");

        Gauge.builder("zeta.livy.alive",
                livyMonitorEndpoint, livyEndpoint -> livyEndpoint.isLivyAlive(cluster.getId()))
                .tags(tagsWithId)
                .description("The Livy instance is alive or not")
                .register(Metrics.globalRegistry);
    }


    @PostConstruct
    public void prometheusRegister() {

        /**
         * Register your own prometheus metrics here
         **/

        // ThreadPoolExecutor status
        registerThreadPoolMetrics(ServerConfigurationContext.webSocketInboundExecutor, "inboundChannel");
        registerThreadPoolMetrics(ServerConfigurationContext.webSocketMessageBrokerExecutor, "messageBroker");
        registerThreadPoolMetrics(ServerConfigurationContext.webSocketOutboundExecutor, "outboundChannel");

        // interpreter monitor
        registerInterpreterMetrics("created");// including dead one
        registerInterpreterMetrics("idle");// including dead one
        registerInterpreterMetrics("running");
        registerInterpreterMetrics("connecting");
        registerInterpreterMetrics("all");

        // Livy statements monitor
        registerLivyStatementMetrics();

        // connection queue status
        registerConnectionQueueMetrics(Cluster.areslvs);
        registerConnectionQueueMetrics(Cluster.herculeslvs);
        registerConnectionQueueMetrics(Cluster.apollorno);

        // Livy instances monitor
        registerLivyInstanceMonitorMetrics(Cluster.areslvs);
        registerLivyInstanceMonitorMetrics(Cluster.herculeslvs);
        registerLivyInstanceMonitorMetrics(Cluster.herculessublvs);
        registerLivyInstanceMonitorMetrics(Cluster.apollorno);
    }

}
