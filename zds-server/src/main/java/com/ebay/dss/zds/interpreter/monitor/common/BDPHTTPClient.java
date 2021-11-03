package com.ebay.dss.zds.interpreter.monitor.common;

import com.ebay.dss.zds.common.HttpUtil;
import com.ebay.dss.zds.common.JsonUtil;
import com.ebay.dss.zds.exception.InvalidNTException;
import com.ebay.dss.zds.interpreter.ConfigurationManager.Cluster;
import com.ebay.dss.zds.interpreter.monitor.modle.*;
import com.ebay.dss.zds.interpreter.monitor.modle.BDPQueueSnapshotResult.BDPQueueSnapshot;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ReadContext;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.stream.Collectors;

public class BDPHTTPClient implements YarnStatusClient {

    protected static final Logger logger = LoggerFactory.getLogger(BDPHTTPClient.class);
    private Cluster cluster;
    private static BDPHTTPClient bdphttpClient;
    private Properties prop;

    // for zeta, filtered some batch:  https://bdp-site.vip.ebay.com/product/userZeta/jnwang
    // for public: https://bdp-site.vip.ebay.com/product/user/
    private String clusterUrl;
    private String queueUrl;

    public BDPHTTPClient(Cluster cluster) {
        this(cluster, null);
    }

    public BDPHTTPClient(Cluster cluster, Properties prop) {
        this.cluster = cluster;
        this.prop = prop;
        setUpBDPUrls();
    }

    private void setUpBDPUrls() {
        if (this.prop == null) this.prop = new Properties();
        this.clusterUrl = prop.getProperty("zds.bdp.cluster.url", "https://bdp-site.vip.ebay.com/product/userZeta");
        this.queueUrl = prop.getProperty("zds.bdp.queue.url", "https://bdp-site.vip.ebay.com/product/queue/user");
    }

    CloseableHttpClient getHttpClient() throws
            KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        //SSLContextBuilder builder = new SSLContextBuilder();
       return HttpUtil.getHttpClient();
    }

    HttpGet httpGet(String url) {
        return HttpUtil.httpGet(url);
    }

    String response2String(CloseableHttpResponse response) throws IOException {
        return HttpUtil.response2String(response);
    }

    String httpGetResource(String url) {
       return HttpUtil.httpGetResource(url);
    }

    List<BDPQueue> parseUserQueues(String content) {
        List<BDPQueue> queues = null;
        if (content != null) {
            try {
                queues = JsonUtil.GSON.fromJson(JsonUtil.jsonParser
                                .parse(content)
                                .getAsJsonObject()
                                .getAsJsonArray("result")
                                .toString(),
                        new TypeToken<List<BDPQueue>>() {
                        }.getType());
            } catch (Exception ex) {
                logger.error("Content json: " + content);
                logger.error(ex.getMessage());
            }
        }
        return queues;
    }

    List<ClusterAccessInfo> parseUserClusters(String content) {
        List<ClusterAccessInfo> clusterAccessInfos = null;
        if (content != null) {
            try {
                JsonArray ja = JsonUtil.jsonParser
                        .parse(content)
                        .getAsJsonObject()
                        .getAsJsonArray("result");
                if (ja.size() > 0) {
                    String clusterAccessStr = ja
                            .get(0)
                            .getAsJsonObject()
                            .get("clusterAccess")
                            .toString();
                    clusterAccessInfos = JsonUtil.GSON.fromJson(clusterAccessStr,
                            new TypeToken<List<ClusterAccessInfo>>() {
                            }.getType());
                }
            } catch (Exception ex) {
                logger.error("Content json: " + content);
                logger.error(ex.getMessage());
            }
        }
        return clusterAccessInfos;
    }

    public List getUserBatchInfo(String clusterName, String userName) {
        String url = String.format("%s/%s", clusterUrl, userName);
        String response = httpGetResource(url);
        if (response != null) {
            Configuration configuration = Configuration.builder().options(Option.SUPPRESS_EXCEPTIONS).build();
            ReadContext ctx = JsonPath.using(configuration).parse(response);
            if (!(ctx.read("$.success", boolean.class) && Optional.ofNullable(ctx.read("$.result[0].clusterAccess", List.class)).isPresent())) {
                throw new InvalidNTException("NT not valid");
            }
            List<String> adminBatchList = ctx.read("$.result[0].adminBatchList[*]");
            List<String> sudoBatchList = ctx.read("$.result[0].sudoerBatchList[*]");
            adminBatchList.addAll(sudoBatchList);
            List<String> noAccessClusters = ctx.read("$.result[0].clusterAccess[?(@.status!='HAS_ACCESS')].clusterName", List.class);
            if (!noAccessClusters.contains(clusterName)) {
                adminBatchList.add(userName);
            }
            List batchInfoList = adminBatchList.stream().distinct().collect(Collectors.toList());
            return batchInfoList.size() > 0 ? batchInfoList : null;
        }
        return null;
    }


    public List<String> getUserQueue(String clusterName, String userName, boolean accessible) {
        String queueAccessUrl = getUserQueueUrl(clusterName, userName);
        String resource = httpGetResource(queueAccessUrl);
        List<BDPQueue> queues = parseUserQueues(resource);
        if (queues != null) {
            List<String> retQueues = queues.stream()
                    .filter(queue -> !accessible || queue.isAccessible())
                    .sorted(Comparator.comparingDouble((BDPQueue bdpQueue) -> bdpQueue.getMaxAbsCapacity() - bdpQueue.getAbsCapacity()).reversed())
                    .map(BDPQueue::getQueueName)
                    .collect(Collectors.toList());
            logger.info("cluster: {}, user: {}, queues: {}", clusterName, userName, retQueues);
            return retQueues.size() > 0 ? retQueues : null;
        }
        return null;
    }

    public List<String> getUserCluster(String userName, boolean accessible) {
        String clusterAccessUrl = getUserClusterUrl(userName);
        String resource = httpGetResource(clusterAccessUrl);
        List<ClusterAccessInfo> clusterAccessInfos = parseUserClusters(resource);
        if (clusterAccessInfos != null) {
            List<String> retClusters = clusterAccessInfos.stream()
                    .filter(cluster -> accessible == cluster.isAccessible())
                    .map(ClusterAccessInfo::getClusterName)
                    .collect(Collectors.toList());
            logger.info("user: {}, clusters: {}", userName, retClusters);
            return retClusters;
        } else return null;
    }

    public String getUserQueueUrl(String clusterName, String userName) {
        return queueUrl + "/" + clusterName + "/" + userName;
    }

    public String getUserClusterUrl(String userName) {
        return clusterUrl+ "/" + userName;
    }

    public String getQueueInfoSnapshotUrl(String clusterName, String queueName) {
        long endTime = System.currentTimeMillis();
        long startTime = endTime - 1000 * 60 * 10;
        return getQueueInfoSnapshotUrl(clusterName, queueName, startTime, endTime, 100);
    }

    public String getQueueInfoSnapshotUrl(String clusterName, String queueName, long startTime, long endTime) {
        return getQueueInfoSnapshotUrl(clusterName, queueName, startTime, endTime, 100);
    }

    public String getQueueInfoSnapshotUrl(String clusterName,
                                          String queueName,
                                          long startTime,
                                          long endTime,
                                          int size) {
        return "https://bdp-site.vip.ebay.com/jpm/api/elastic/search?" +
                "query=running_queue_metrics%5b@site=%22" + clusterName + "%22," +
                "@snapTimestamp=%5b" + startTime + "," + endTime + "%5d" +
                (queueName == null ? "" : ",@queueName=%22" + queueName + "%22") + "%5d&size=" + size;
    }

    public String getQueueUserUsageUrl(String clusterName, String queueName, long start, long end) {
        return "https://bdp-site.vip.ebay.com/jpm/api/elastic/search?query=running_queue_user_metrics[@site=%22"
                + clusterName
                + "%22,@queue=%22" + queueName + "%22,@snapTimestamp=[" + start + "," + end + "]]%3C@user,[sum(allocatedMB),sum(clusterUsagePercentage)]%3E&size=0";
    }

    public YarnQueue parseYarnQueue(String content) {
        JsonObject queueTrend = JsonUtil.jsonParser.parse(content).getAsJsonObject();
        boolean success = queueTrend.get("success").getAsBoolean();
        if (success) {
            JsonArray usageList = queueTrend
                    .getAsJsonArray("data")
                    .get(0).getAsJsonObject().get("data").getAsJsonArray();
            int index = usageList.size() - 1;
            JsonArray usageRow = usageList.get(index).getAsJsonArray();
            double usage = usageRow.get(usageRow.size() - 1).getAsDouble();
            Iterator<JsonElement> capacityList = queueTrend
                    .getAsJsonArray("capacity")
                    .iterator();
            JsonObject maxCapacityObj = null;
            while (capacityList.hasNext()) {
                JsonObject capacityRow = capacityList.next().getAsJsonObject();
                if ("Max Capacity".equals(capacityRow.get("name").getAsString())) {
                    maxCapacityObj = capacityRow;
                    break;
                }
            }

            if (maxCapacityObj != null) {
                JsonArray capacityDataRow = maxCapacityObj.getAsJsonArray("data")
                        .get(index).getAsJsonArray();
                double maxCapacity = capacityDataRow.get(capacityDataRow.size() - 1).getAsDouble();
                YarnQueue yarnQueue = new YarnQueue();
                yarnQueue.setAbsoluteUsedCapacity(usage);
                yarnQueue.setAbsoluteMaxCapacity(maxCapacity);
                return yarnQueue;
            } else {
                logger.error("Can't find max capacity");
                return null;
            }

        } else {
            logger.error(queueTrend.get("message").getAsString());
            return null;
        }
    }

    public YarnQueue getYarnQueue(String clusterName, String queueName) {
        try {
            long endTime = System.currentTimeMillis();
            long startTime = endTime - 1000 * 60 * 10;
            String json = httpGetResource(getQueueInfoSnapshotUrl(clusterName, queueName, startTime, endTime));
            BDPQueueSnapshotResult bdpQueueSnapshotResult = BDPQueueSnapshotResult.fromJson(json);
            BDPQueueSnapshot bdpQueueSnapshot = bdpQueueSnapshotResult.getFirstLatestSnapshot();
            if (bdpQueueSnapshot == null) return null;
            YarnQueue yarnQueue = bdpQueueSnapshot.toYarnQueue();
            yarnQueue.setUsers(BDPQueueUsage.toMap(httpGetResource(getQueueUserUsageUrl(clusterName, queueName, startTime, endTime))));
            return yarnQueue;
        } catch (Exception ex) {
            logger.error("failed to get queue info: [cluster: "
                    + this.cluster + ", queue: " + queueName + "] "
                    + ex.toString());
            return null;
        }
    }

    public YarnQueue getQueue(String queue) {
        return getYarnQueue(this.cluster.name(), queue);
    }

    public List<YarnQueue> getQueues(String clusterName) {
        try {
            List<YarnQueue> yarnQueues;
            long endTime = System.currentTimeMillis();
            long startTime = endTime - 1000 * 60 * 10;
            String json = httpGetResource(getQueueInfoSnapshotUrl(clusterName, null, startTime, endTime));
            BDPQueueSnapshotResult bdpQueueSnapshotResult = BDPQueueSnapshotResult.fromJson(json);
            List<BDPQueueSnapshot> bdpQueueSnapshots = bdpQueueSnapshotResult.getLatestResult();
            if (bdpQueueSnapshots == null || bdpQueueSnapshots.size() == 0) return null;
            yarnQueues = bdpQueueSnapshots
                    .stream()
                    .map(BDPQueueSnapshot::toYarnQueue)
                    .collect(Collectors.toList());
            return yarnQueues;
        } catch (Exception ex) {
            logger.error("failed to get queues info: [cluster: "
                    + this.cluster + "] "
                    + ex.toString());
            return null;
        }
    }

    public List<YarnQueue> getQueues() {
        return getQueues(this.cluster.name());
    }

    public static BDPHTTPClient nonStatusInstance() {
        if (bdphttpClient == null) {
            synchronized (BDPHTTPClient.class) {
                bdphttpClient = new BDPHTTPClient(null);
            }
        }
        return bdphttpClient;
    }

}
