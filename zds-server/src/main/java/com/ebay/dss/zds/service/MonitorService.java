package com.ebay.dss.zds.service;

import com.ebay.dss.zds.dao.ZetaNotebookRepository;
import com.ebay.dss.zds.interpreter.ConfigurationManager;
import com.ebay.dss.zds.interpreter.ConfigurationManager.Cluster;
import com.ebay.dss.zds.interpreter.InterpreterConfiguration;
import com.ebay.dss.zds.interpreter.InterpreterManager;
import com.ebay.dss.zds.interpreter.InterpreterType;
import com.ebay.dss.zds.interpreter.interpreters.Interpreter;
import com.ebay.dss.zds.interpreter.interpreters.InterpreterGroup;
import com.ebay.dss.zds.interpreter.monitor.common.ClusterStatus;
import com.ebay.dss.zds.interpreter.monitor.common.ClusterStatusFactory;
import com.ebay.dss.zds.interpreter.monitor.modle.YarnQueue;
import com.ebay.dss.zds.model.ZetaNotebook;
import com.ebay.dss.zds.message.event.ZetaMonitorEvent;
import com.ebay.dss.zds.message.status.LivyStatementStatusStore;
import com.ebay.dss.zds.message.status.persistence.InfluxDBPersistence;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;
import java.util.*;

@Service
public class MonitorService {

  private static final Logger logger = LoggerFactory.getLogger(MonitorService.class);
  private static final HashMap<Integer, ClusterStatus> clusterMap = new HashMap<>();
  private static final HashMap<Integer, QueueKeyProvider> queueKeyProviderMap = new HashMap<>();
  private static final JsonParser parser = new JsonParser();

  private ZetaNotebookRepository zetaNotebookRepository;

  private ConfigurationManager configurationManager;

  private InterpreterManager interpreterManager;

  private ClusterStatusFactory clusterStatusFactory;

  private LivyStatementStatusStore livyStatementStatusStore;

  private volatile boolean dynamicMonitorEnabled = false;

  public MonitorService() {
  }

  @Autowired
  public MonitorService(ZetaNotebookRepository zetaNotebookRepository,
                        ConfigurationManager configurationManager,
                        InterpreterManager interpreterManager) {
    this.zetaNotebookRepository = zetaNotebookRepository;
    this.configurationManager = configurationManager;
    this.interpreterManager = interpreterManager;
  }

  void addCluster(ConfigurationManager.Cluster cluster, InterpreterConfiguration conf, String method, QueueKeyProvider provider) {
    ClusterStatus cs = clusterStatusFactory.getClusterStatus(conf.getProperties(), cluster, method);
    clusterMap.put(cluster.getId(), cs);
    queueKeyProviderMap.put(cluster.getId(), provider);
  }

  ClusterStatusFactory getClusterStatusFactory(Properties prop) {
    return ClusterStatusFactory.getInstance(prop);
  }

  private QueueKeyProvider livyQueueKeyProvider = this::getQueueNameFromNotebookPreference;

  @PostConstruct
  void init() {
    InterpreterConfiguration conf = configurationManager.getDefaultConfiguration();
    clusterStatusFactory = getClusterStatusFactory(conf.getProperties());

    // Hercules
    addCluster(Cluster.herculeslvs, conf, ClusterStatusFactory.BDP, livyQueueKeyProvider);
    // Ares
    addCluster(Cluster.areslvs, conf, ClusterStatusFactory.BDP, livyQueueKeyProvider);
    // Apollo-phx
    // addCluster(Cluster.apollophx, conf, ClusterStatusFactory.DENGINE);
    // Hercules-sub
    addCluster(Cluster.herculessublvs, conf, ClusterStatusFactory.BDP, livyQueueKeyProvider);
    // Apollo-rno
    addCluster(Cluster.apollorno, conf, ClusterStatusFactory.BDP, livyQueueKeyProvider);

    livyStatementStatusStore = new LivyStatementStatusStore();
    try {
      InfluxDBPersistence influxDBPersistence = new InfluxDBPersistence(configurationManager.getEnvironment());
      livyStatementStatusStore.addPersistence(influxDBPersistence);
    } catch (Exception ex) {
      logger.warn("Failed to add persistence to livyStatementStatusStore...");
    }

    dynamicMonitorEnabled = Boolean.valueOf(configurationManager
            .getEnvironment()
            .getProperty("zds.track.dynamic.monitor.enabled", "false"));
  }

  public List<YarnQueue> fetchQueues(Integer clusterId, String key) throws RuntimeException {
    logger.info("fetching... cluster: [{}], key: [{}]", clusterId, key);
    ClusterStatus status = Optional.ofNullable(clusterMap.get(clusterId))
            .orElseThrow(() -> new RuntimeException("Please provide invalid cluster Id"));
    return key == null ? status.getYarnQueues() : Arrays.asList(status.getYarnQueue(key));
  }

  @Deprecated
  public List<YarnQueue> fetchQueues(String noteId, String nt) throws RuntimeException {
    ZetaNotebook note = Optional.ofNullable(zetaNotebookRepository.searchNotebook(noteId))
            .orElseThrow(() -> new RuntimeException("Can't find any notebook by id: " + noteId));

    String configId = getConfigFileId(note.getPreference());

    ZetaNotebook config = InterpreterConfiguration.DEFAULT_CONFIG_OBJECT;
    Integer clusterId = Cluster.herculeslvs.getId();
    if (!InterpreterConfiguration.DEFAULT_REFERENCE_KEY.equals(configId)) {
      JsonElement connectionObject = parser.parse(note.getPreference())
              .getAsJsonObject()
              .get("notebook.connection");
      clusterId = connectionObject == null ? clusterId :
              connectionObject.getAsJsonObject()
                      .get("clusterId")
                      .getAsInt();
      config = Optional.ofNullable(zetaNotebookRepository.searchNotebook(configId))
              .orElseThrow(() -> new RuntimeException("Can't find any configuration by id: " + configId));
    }

    String queueKey;
    queueKey = getQueueName(config.getContent());

    return fetchQueues(clusterId, queueKey);

  }

  public List<YarnQueue> fetchQueues(String noteId, Integer clusterId, String nt) throws RuntimeException {
    QueueKeyProvider provider = this.queueKeyProviderMap.get(clusterId);
    return fetchQueues(clusterId, provider.provideKey(noteId, nt));

  }

  private String getQueueNameFromNotebookPreference(String noteId, String nt) {
    ZetaNotebook note = Optional.ofNullable(zetaNotebookRepository.searchNotebook(noteId))
            .orElseThrow(() -> new RuntimeException("Can't find any notebook by id: " + noteId));

    String configId = getConfigFileId(note.getPreference());

    ZetaNotebook config = InterpreterConfiguration.DEFAULT_CONFIG_OBJECT;
    if (!InterpreterConfiguration.DEFAULT_REFERENCE_KEY.equals(configId)) {

      config = Optional.ofNullable(zetaNotebookRepository.searchNotebook(configId))
              .orElseThrow(() -> new RuntimeException("Can't find any configuration by id: " + configId));
    }
    return getQueueName(config.getContent());
  }

  public String getConfigFileId(String preference) {
    JsonElement configObject = parser.parse(preference)
            .getAsJsonObject()
            .get(InterpreterConfiguration.NOTEBOOK_PROFILE_KEY_NAME);
    return configObject != null ? configObject.getAsString() :
            InterpreterConfiguration.DEFAULT_REFERENCE_KEY;
  }


  public String getQueueName(String preference) {
    return parser.parse(preference)
            .getAsJsonObject()
            .get(InterpreterType.ConfType.ZDS_LIVY.getName())
            .getAsJsonObject()
            .get("zds.livy.spark.yarn.queue").getAsString();
  }

  public void handleMonitorEvent(ZetaMonitorEvent event) {

   if (event instanceof ZetaMonitorEvent.LivyStatementMonitorEvent) {
      if (!dynamicMonitorEnabled) return;
      handleLivyMonitorEvent((ZetaMonitorEvent.LivyStatementMonitorEvent) event);
    } else {
      // todo: implement more monitor event
    }
  }

  private void handleLivyMonitorEvent(ZetaMonitorEvent.LivyStatementMonitorEvent event) {
    livyStatementStatusStore.flushAndPersist(event);
  }


  public LivyStatementStatusStore getLivyStatementStatusStore() {
    return livyStatementStatusStore;
  }

  public ConfigurationManager getConfigurationManager() {
    return configurationManager;
  }

  public void toggleDynamicMonitor(boolean flag) {
    this.dynamicMonitorEnabled = flag;
  }

  private interface QueueKeyProvider {
    String provideKey(String noteId, String nt);
  }
}
