package com.ebay.dss.zds.service;

import com.ebay.dss.zds.common.JsonUtil;
import com.ebay.dss.zds.dao.ZetaCacheRepository;
import com.ebay.dss.zds.interpreter.ConfigurationManager;
import com.ebay.dss.zds.interpreter.InterpreterConfiguration;
import com.ebay.dss.zds.interpreter.ConfigurationManager.Cluster;
import com.ebay.dss.zds.interpreter.monitor.common.BDPHTTPClient;
import com.ebay.dss.zds.interpreter.monitor.modle.YarnQueue;
import com.ebay.dss.zds.model.ZetaCache;
import com.ebay.dss.zds.message.EventTracker;
import com.ebay.dss.zds.message.event.ZetaMonitorEvent;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.ebay.dss.zds.message.event.Event.YarnQueueMonitorEvent;


@Service
public class BDPHTTPService {

    private final static Logger logger = LoggerFactory.getLogger(BDPHTTPService.class);

    @Value("${zds.yarn.queue.capacity.threshold}")
    private double queueCapacityThreshold;

    @Value("${zds.yarn.queue.busy.max-count}")
    private int queueCapacityMaxBusyCount;

    @Value(("${zds.monitor.receiver}"))
    private String emailReceiver;

    private BDPHTTPClient bdphttpClient;

    private ZetaCacheRepository zetaCacheRepository;

    private Map<Cluster, YarnQueueStatusStore> yarnQueueStatusStoreMap = new HashMap<>();

    @Autowired
    public BDPHTTPService(ZetaCacheRepository zetaCacheRepository) {
        this.zetaCacheRepository = zetaCacheRepository;
        this.bdphttpClient = new BDPHTTPClient(null);
    }

    public void setBdphttpClient(BDPHTTPClient bdphttpClient) {
        this.bdphttpClient = bdphttpClient;
    }

    @Cacheable(value = "queueinfo", unless = "#result==null")
    public YarnQueue getQueueInfo(String clusterName, String queueName) {
        Object queueInfo = Optional
                .ofNullable(getBDPInfoFromAPI("queueinfo", clusterName, queueName))
                .orElseGet(() -> getBDPInfoFromDB("queueinfo", clusterName, queueName, YarnQueue.class));
        logger.info("Put [{}] queue info in cluster [{}] to cache", queueName, clusterName);
        return Objects.nonNull(queueInfo) ? (YarnQueue) queueInfo : null;
    }

    @Cacheable(value = "sudoerbatch", unless = "#result==null")
    public List<String> getUserBatchInfo(String clusterName, String userName) {
        Object userBatchInfo = Optional
                .ofNullable(getBDPInfoFromAPI("sudoerbatch", clusterName, userName))
                .orElseGet(() -> getBDPInfoFromDB("sudoerbatch", clusterName, userName, List.class));
        logger.info("Put User [{}] batch info in cluster [{}] to cache", userName, clusterName);
        return Objects.nonNull(userBatchInfo) ? (List<String>) userBatchInfo : null;

    }

    @Cacheable(value = "userqueue", unless = "#result==null")
    public List<String> getUserQueueInfo(String clusterName, String userName) {
        Object userQueueInfo = Optional
                .ofNullable(getBDPInfoFromAPI("userqueue", clusterName, userName))
                .orElseGet(() -> getBDPInfoFromDB("userqueue", clusterName, userName, List.class));
        logger.info("Put User [{}] queue info in cluster [{}] to cache", userName, clusterName);
        return Objects.nonNull(userQueueInfo) ? (List<String>) userQueueInfo : null;
    }

    @Cacheable(value = "usercluster", unless = "#result==null")
    public List<String> getUserClusterInfo(String userName) {
        Object userClusterInfo = Optional
                .ofNullable(getBDPInfoFromAPI("usercluster", "all", userName))
                .orElseGet(() -> getBDPInfoFromDB("usercluster", "all", userName, List.class));
        logger.info("Put User [{}] cluster info [{}] to cache", userName);
        return Objects.nonNull(userClusterInfo) ? (List<String>) userClusterInfo : null;
    }

    private <T> T getBDPInfoFromAPI(String type, String clusterName, String info) {
        logger.info("Get BDP info from API: type [{}], cluster [{}], info [{}]", type, clusterName, info);
        Object bdpInfo = null;
        switch (type) {
            case "queueinfo":
                bdpInfo = bdphttpClient.getYarnQueue(clusterName, info);
                break;
            case "sudoerbatch":
                bdpInfo = bdphttpClient.getUserBatchInfo(clusterName, info);
                break;
            case "userqueue":
                bdpInfo = bdphttpClient.getUserQueue(clusterName, info, true);
                break;
            case "usercluster":
                bdpInfo = bdphttpClient.getUserCluster(info, true);
            default:
                break;
        }
        if (Objects.nonNull(bdpInfo)) {
            String key = String.format("%s-%s-%s", type, clusterName, info);
            ZetaCache zetaCache = new ZetaCache();
            zetaCache.setCreateDt(new Date());
            zetaCache.setKeyName(key);
            zetaCache.setValue(JsonUtil.toJson(bdpInfo));
            zetaCacheRepository.save(zetaCache);
            return (T) bdpInfo;
        }
        logger.warn("GET BDP info from API Failed! type [{}], cluster [{}], info [{}]", type, clusterName, info);
        return null;
    }

    private <T> T getBDPInfoFromDB(String type, String clusterName, String info, Class clazz) {
        logger.info("Get BDP info from DB: type [{}], cluster [{}], info [{}]", type, clusterName, info);
        String keyName = String.format("%s-%s-%s", type, clusterName, info);
        ZetaCache zetaCache = zetaCacheRepository.findByKeyName(keyName);
        if (Objects.nonNull(zetaCache)) {
            return (T) JsonUtil.fromJson(zetaCache.getValue(), clazz);
        }
        logger.warn("GET BDP info from DB Failed! type [{}], cluster [{}], info [{}]", type, clusterName, info);
        return null;
    }

    public YarnQueue getYarnQueue(String clusterName, String queueName) {
        return bdphttpClient.getYarnQueue(clusterName, queueName);
    }

    public YarnQueue getZetaConnectionQueue(String clusterName) {
        String queueName = InterpreterConfiguration
                .getInitedConfiguration()
                .getProperty("zds.livy.connectionQueue.name");
        if (StringUtils.isNotEmpty(queueName)) {
            return getYarnQueue(clusterName, queueName);
        } else {
            logger.info("No connection queue provided just skip");
            return null;
        }
    }

    //@Scheduled(fixedDelay = 5 * 60 * 1000)
    @Deprecated
    public void scheduleMonitorZetaConnectionQueue() {
        String queueName = InterpreterConfiguration
                .getInitedConfiguration()
                .getProperty("zds.livy.connectionQueue.name");
        if (StringUtils.isNotEmpty(queueName)) {
            logger.info("Monitor connection queue: {}'s status...", queueName);
            Arrays.stream(ConfigurationManager.Cluster.values())
                    .forEach(cluster -> checkAndSendEmail(cluster, queueName));
            logger.info("Monitoring finished");
        } else logger.info("No connection queue provided just skip");
    }

    @Deprecated
    public void checkAndSendEmail(Cluster cluster, String queueName) {
        logger.info("Checking cluster: {}, queue: {}", cluster.name(), queueName);
        YarnQueueStatusStore yarnQueueStatusStore = yarnQueueStatusStoreMap
                .getOrDefault(cluster, new YarnQueueStatusStore(getQueueCapacityThreshold(),
                        getQueueCapacityMaxBusyCount()));
        YarnQueue yarnQueue = getYarnQueue(cluster.name(), queueName);
        if (yarnQueue == null) {
            logger.error("Got empty yarn queue information from bdp for cluster: {} and queue: {}, skip...",
                    cluster.name(), queueName);
            return;
        }
        if (yarnQueueStatusStore.refreshBy(yarnQueue)) { // busy, post event
            String text = "The queue: " + queueName
                    + " " + " in " + cluster.name()
                    + " is overused! (capacity > "
                    + queueCapacityThreshold + ")";
            if (!yarnQueueStatusStore.emailSent) {
                ZetaMonitorEvent.YarnQueueMonitorEvent event = YarnQueueMonitorEvent(cluster.name(), queueName, text);
                event.getExternalContext().setProperty("subject", text);
                event.getExternalContext().setProperty("emailReceiver", emailReceiver);
                EventTracker.postEvent(event);
                yarnQueueStatusStore.emailSent = true;
            } else logger.info("Skip already sent: " + text);
        }
        yarnQueueStatusStoreMap.put(cluster, yarnQueueStatusStore);
        logger.info("Finished checking cluster: {}, queue: {}", cluster.name(), queueName);
    }

    public double getQueueCapacityThreshold() {
        return queueCapacityThreshold;
    }

    public int getQueueCapacityMaxBusyCount() {
        return queueCapacityMaxBusyCount;
    }

    protected void setQueueCapacityThreshold(double queueCapacityThreshold) {
        this.queueCapacityThreshold = queueCapacityThreshold;
    }

    protected void setQueueCapacityMaxBusyCount(int queueCapacityMaxBusyCount) {
        this.queueCapacityMaxBusyCount = queueCapacityMaxBusyCount;
    }

    public String getEmailReceiver() {
        return emailReceiver;
    }

    protected void setEmailReceiver(String emailReceiver) {
        this.emailReceiver = emailReceiver;
    }

    public static class YarnQueueStatusStore {
        private boolean isBusy = false;
        int busyCnt = 0;
        double threshold;
        int maxBusyCnt;
        protected volatile boolean emailSent = false;

        public YarnQueueStatusStore(double threshold, int maxBusyCnt) {
            this.threshold = threshold;
            this.maxBusyCnt = maxBusyCnt;
        }

        // return isBusy
        public boolean refreshBy(YarnQueue newSnapshot) {
            if (newSnapshot.getUsedPct() < threshold) {
                isBusy = false;
                busyCnt = 0;
                emailSent = false;
            } else {
                busyCnt++;
                if (busyCnt >= maxBusyCnt) {
                    isBusy = true;
                }
            }
            return isBusy;
        }

    }

}
