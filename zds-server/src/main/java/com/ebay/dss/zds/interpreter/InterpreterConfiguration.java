package com.ebay.dss.zds.interpreter;

import com.ebay.dss.zds.common.ClassUtil;
import com.ebay.dss.zds.dao.ZetaNotebookRepository;
import com.ebay.dss.zds.exception.ConfigurationNotFoundException;
import com.ebay.dss.zds.interpreter.annotation.HermesBackend;
import com.ebay.dss.zds.interpreter.interpreters.jdbc.IJdbcConf;
import com.ebay.dss.zds.interpreter.interpreters.livy.LivyClientBuilder;
import com.ebay.dss.zds.kerberos.KerberosContext;
import com.ebay.dss.zds.model.ZetaNotebook;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.yarn.webapp.hamlet.Hamlet;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.ebay.dss.zds.interpreter.InterpreterType.getConfType;
import static com.ebay.dss.zds.interpreter.interpreters.jdbc.Constant.JDBC_CONF_KEY;

import com.ebay.dss.zds.interpreter.ConfigurationManager.Cluster;

/**
 * Created by tatian on 2018/5/23.
 */
public class InterpreterConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(InterpreterConfiguration.class);
    private Properties prop;
    private InterpreterType.ConfType realm;
    private static String configPath;
    protected static InterpreterConfiguration initedConfiguration;
    private static ConfigurationManager configurationManager;
    private static ConcurrentHashMap<Integer, Boolean> useConnQueue = new ConcurrentHashMap<>();
    public static final String MAX_VIEW_RESULT = "zds.view.maxResult";
    public static final String APPLIED_PACKAGES_LIST = "zds.packages.applied.list";

    public static void setUseConnQueue(int clusterId, boolean enabled) {
        logger.info(String.format("set useConnQueue, clusterId: %s, enabled: %s", clusterId, enabled));
        useConnQueue.put(clusterId, enabled);
    }

    public static Map<Integer, Boolean> getUseConnQueue() {
        return useConnQueue;
    }

    public static InterpreterConfiguration loadFromDefaultPath(){
        return configPath!=null? new InterpreterConfiguration(configPath):new InterpreterConfiguration();
    }

    public InterpreterConfiguration(String configPath) {
        this.prop = new Properties();
        load(configPath);
    }

    public InterpreterConfiguration(Properties prop) {
        this.prop = prop;
    }

    public InterpreterConfiguration() {
        this.prop = new Properties();
    }

    public void initForJDBC(String noteId, String realUser, String userName, ZetaNotebookRepository repo, String className, Map<String, String> prop) {
        logger.info("Loading configuration from db... className: {}, props: {}", className, prop.toString());
        try {
            // todo: handle not owner part
            String preference = prop.get("preference");
            ZetaNotebook notebook = null;
            try {
                notebook = repo.getNotebook(noteId);
            } catch (Exception ex) {
                logger.error("Failed to search notebook with id: {}, error: {}", noteId, ex);
            }
            if (StringUtils.isEmpty(preference)) {
                if (notebook != null && notebook.ownedBy(realUser)) {
                    preference = notebook.getPreference();
                } else {
                    logger.info("User: {} is not the owner of notebook: {}, and the preference is empty, using default config", realUser, noteId);
                    loadFromString(InterpreterConfiguration.DEFAULT_SPARK_CONFIG, className);
                    return;
                }
            }
            if (!StringUtils.isEmpty(preference)) {
                JSONObject pre = new JSONObject(preference);
                if(pre.has(InterpreterConfiguration.NOTEBOOK_PROFILE_KEY_NAME)) {
                    String reference = pre.getString(InterpreterConfiguration.NOTEBOOK_PROFILE_KEY_NAME);
                    if (!"default".equals(reference)) {
                        logger.info("Getting configuration from reference file: {}", reference);
                        ZetaNotebook config = repo.searchNotebook(reference);
                        if (config != null) {
                            loadFromString(config.getContent(), className);
                        } else {
                            logger.info("Using default reference file for {}", className);
                            loadFromString(InterpreterConfiguration.DEFAULT_SPARK_CONFIG, className);
                        }
                    } else {
                        logger.info("Using default reference file for {}", className);
                        loadFromString(InterpreterConfiguration.DEFAULT_SPARK_CONFIG, className);
                    }
                }
            } else logger.info("Didn't find preference key!");
        } catch (Exception ex) {
            throw new ConfigurationNotFoundException("Failed to init configuration, exception: " + ex.getMessage());
        }
        logger.info("Loading done");
    }

    // todo: reconstruct
    public void init(ZetaNotebookRepository repo, String noteId, String className,
                     String realUser, String userName, Integer clusterId, boolean useDynamicQueue, Map<String, String> properties) {
        logger.info("Loading configuration from db... className: {}", className);
        try {
            String preference = properties.get("preference");
            ZetaNotebook note = null;
            try {
                note = repo.searchNotebook(noteId);
            } catch (Exception ex) {
                logger.error("Failed to search notebook with id: {}, error: {}", noteId, ex);
            }

            if (StringUtils.isEmpty(preference)) {
                if (note != null && note.ownedBy(realUser)) {
                    preference = note.getPreference();
                } else {
                    logger.info("User: {} is not the owner of notebook: {}, and the preference is empty, using default config", realUser, noteId);
                    loadFromString(getDefaultSparkConf(clusterId, userName, useDynamicQueue), className);
                    return;
                }
            }

            if (!StringUtils.isEmpty(preference)) {
                String referenceKey = InterpreterConfiguration.DEFAULT_REFERENCE_KEY;
                JSONObject pre = new JSONObject(preference);
                if (pre.has(InterpreterConfiguration.NOTEBOOK_PROFILE_KEY_NAME)) {
                    referenceKey = pre.getString(InterpreterConfiguration.NOTEBOOK_PROFILE_KEY_NAME);
                }
                if (StringUtils.isEmpty(referenceKey) || referenceKey.equals(InterpreterConfiguration.DEFAULT_REFERENCE_KEY)) {
                    loadFromString(getDefaultSparkConf(clusterId, userName, useDynamicQueue), className);
                } else {
                    ZetaNotebook config = repo.searchNotebook(referenceKey);
                    if (config == null) {
                        loadFromString(getDefaultSparkConf(clusterId, userName, useDynamicQueue), className);
                    } else {
                        loadFromString(config.getContent(), className);
                        if (prop.getProperty("zds.livy.spark.yarn.queue",
                                DYNAMIC_QUEUE_PATTERN).contains(DYNAMIC_QUEUE_PATTERN)) {
                            String dynamicQueue = getDynamicQueue(clusterId, userName);
                            if (StringUtils.isNotEmpty(dynamicQueue)) {
                                prop.setProperty("zds.livy.spark.yarn.queue", dynamicQueue);
                                logger.info("using dynamic queue: {} for " +
                                                "user: {}, noteId: {}, cluster: {}",
                                        dynamicQueue, userName, noteId, clusterId);
                            } else {
                                logger.error("Failed to get dynamic queue for " +
                                                "user: {}, noteId: {}, cluster: {}, using default: {}",
                                        userName, noteId, clusterId, DEFAULT_LIVY_SPARK_QUEUE);
                                prop.setProperty("zds.livy.spark.yarn.queue", DEFAULT_LIVY_SPARK_QUEUE);
                            }
                        }
                    }
                }
                String packages = note!= null? note.getPackages() : null;
                if (StringUtils.isNotEmpty(packages)) {
                    prop.put(APPLIED_PACKAGES_LIST, packages);
                    logger.info("Set new packages in interpreter configuration: " + packages);

                }
            }
        } catch (Exception ex) {
            throw new ConfigurationNotFoundException("Failed to init configuration, exception: " + ex.getMessage());
        }
        logger.info("Loading done");
    }

    public void load(String configPath) {
        logger.info("Loading configuration from files: {}", configPath);
        try {
            if (configPath.contains("interpreter-dev.properties")) {
                try {
                    prop.load(new FileInputStream(new File(getClass().getResource("/interpreter-dev.properties").getFile())));
                } catch (FileNotFoundException ex) {
                    prop.load(new FileInputStream(new File(configPath)));
                }
            } else {
                prop.load(new FileInputStream(new File(configPath)));
            }
        } catch (IOException ex) {
            throw new ConfigurationNotFoundException(ex.getMessage());
        }
        logger.info("Loading done");
    }

    public void loadFromString(String confStr, String className) {
        logger.info("Loading configuration from String...");
        if (!StringUtils.isEmpty(confStr)) {
            JSONObject up = new JSONObject(confStr);
            String confType = getConfType(className).getName();
            if (up.has(confType)) {
                JSONObject rt = up.getJSONObject(confType);
                Iterator it = rt.keys();
                while (it.hasNext()) {
                    String key = (String) it.next();
                    String value = rt.get(key).toString();
                    if (!StringUtils.isEmpty(value) && !value.equalsIgnoreCase("null")) {
                        prop.put(StringUtils.strip(key), value);
                    }
                }
            }
        }
        logger.info("Loading done");
    }

    private String getDefaultSparkConf(Integer clusterId, String userName, boolean useDynamicQueue) {
        if (useDynamicQueue) {
            return getDynamicSparkConf(clusterId, userName);
        } else return DEFAULT_SPARK_CONFIG;
    }

    public void merge(InterpreterConfiguration conf) {
        conf.getProperties()
                .keySet()
                .forEach(key ->
                    prop.put(key, conf.getProperties().get(key))
                );
    }

    public void merge(Properties properties) {
        properties
                .keySet()
                .forEach(key ->
                    prop.put(key, properties.get(key))
                );
    }

    public void merge(Map<String, String> properties) {
        this.prop.putAll(properties);
    }

    public String getProperty(String key) {
        String value = prop.getProperty(key);
        if (value == null && realm != null && realm != InterpreterType.ConfType.UNKNOWN) {
           return prop.getProperty(realm.getName() + "." + key);
        } else return value;
    }

    public String getOrDefault(String key, String defaultValue) {
        String value = getProperty(key);
        return value == null? defaultValue : value;
    }

    public void setProperty(String key, String value) {
        prop.setProperty(key, value);
    }

    public void removeProperty(String key) {
        prop.remove(key);
    }

    public boolean containsKey(Object key) {
        return prop.containsKey(key);
    }

    public Properties getProperties() {
        return prop;
    }

    public InterpreterConfiguration mapClusterIdToUrl(Integer clusterId) {
        LivyClientBuilder.mapClusterIdToUrl(prop, clusterId);
        return this;
    }

    public InterpreterType.ConfType getRealm() {
        return realm;
    }

    public void setRealm(InterpreterType.ConfType realm) {
        this.realm = realm;
    }

    public static String getConfigPath() {
        return configPath;
    }

    public static void setConfigPath(String configPath) {
        InterpreterConfiguration.configPath = configPath;
    }

    public static InterpreterConfiguration getInitedConfiguration() {
        return initedConfiguration;
    }

    public static void setConfigurationManager(ConfigurationManager configurationManager) {
        InterpreterConfiguration.configurationManager = configurationManager;
    }

    public Optional<KerberosContext> getKerberosContext() {
        KerberosContext context = null;
        Object maybeJdbcConf = this.prop.get(JDBC_CONF_KEY);
        if (maybeJdbcConf instanceof IJdbcConf) {
            IJdbcConf conf = (IJdbcConf) maybeJdbcConf;
            Object maybeKerberosContext = conf.get("KerberosContext");
            if (maybeKerberosContext instanceof KerberosContext) {
                context = (KerberosContext) maybeKerberosContext;
            } else {
                logger.warn("Can't find a kerberos context!");
            }
        } else {
            logger.warn("Not a jdbc config, not kerberos context provided");
        }
        return Optional.ofNullable(context);
    }

    /*
    public static DynamicConfigurationContext getDynamicConfigurationContext() {
        return dynamicConfigurationContext;
    }*/

    public final static String DYNAMIC_QUEUE_PATTERN = "usingDynamicQueue";

    public final static String DEFAULT_REFERENCE_KEY = "default";

    public final static String NOTEBOOK_PROFILE_KEY_NAME = "notebook.profile";

    public final static String NOTEBOOK_VIEW_MAX_ROW = "zds.view.maxResult";

    public final static String DEFAULT_NOTEBOOK_PREFERENCE = "{\n" +
            "   \"" + NOTEBOOK_PROFILE_KEY_NAME + "\":\"" + DEFAULT_REFERENCE_KEY + "\"\n" +
            "}";

    public final static String DEFAULT_LIVY_SPARK_QUEUE = "hdlq-data-default";
    public final static String DEFAULT_SPARK_CONFIG = "{\n" +
            "\t\"zds.livy\": {\n" +
            "\t\t\"zds.livy." + NOTEBOOK_VIEW_MAX_ROW + "\": \"" + 1000 + "\",\n" +
            "\t\t\"zds.livy.spark.sql.join.statisticOnExecution\": \"false\",\n" +
            "\t\t\"zds.livy.spark.yarn.queue\": \"" + DEFAULT_LIVY_SPARK_QUEUE + "\",\n" +
            "\t\t\"zds.livy.spark.driver.memory\": \"8g\",\n" +
            "\t\t\"zds.livy.spark.executor.cores\": \"3\",\n" +
            "\t\t\"zds.livy.spark.executor.memory\": \"4g\",\n" +
            "\t\t\"zds.livy.spark.yarn.executor.memoryOverhead\": \"1024\",\n" +
            "\t\t\"zds.livy.spark.yarn.am.extraJavaOptions\": \"\",\n" +//-XX:MaxMetaspaceSize=128m -XX:MaxDirectMemorySize=128m
            "\t\t\"zds.livy.spark.executor.extraJavaOptions\": \"\",\n" +//-XX:MaxMetaspaceSize=128m -XX:MaxDirectMemorySize=128m
            "\t\t\"zds.livy.spark.driver.extraJavaOptions\": \"\",\n" +
            "\t\t\"zds.livy.spark.sql.shuffle.partitions\": \"500\",\n" +
            "\t\t\"zds.livy.spark.rdd.compress\": \"true\",\n" +
            //"\t\t\"zds.livy.spark.sql.autoBroadcastJoinThreshold\": \"104857600\",\n" +
            "\t\t\"zds.livy.spark.dynamicAllocation.initialExecutors\": \"1\",\n" +
            "\t\t\"zds.livy.spark.dynamicAllocation.minExecutors\": \"0\",\n" +
            "\t\t\"zds.livy.spark.dynamicAllocation.maxExecutors\": \"1000\",\n" +
            "\t\t\"zds.livy.spark.sql.caseSensitive\": \"false\",\n" +
            "\t\t\"zds.livy.spark.sql.crossJoin.enabled\": \"true\",\n" +
            "\t\t\"zds.livy.spark.sql.parquet.writeLegacyFormat\": \"true\",\n" +
            "\t\t\"zds.livy.hive.exec.dynamic.partition\": \"true\",\n" +
            "\t\t\"zds.livy.hive.exec.dynamic.partition.mode\": \"nonstrict\",\n" +
            "\t\t\"zds.livy.spark.speculation\": \"true\",\n" +
            "\t\t\"zds.livy.spark.speculation.quantile\": \"0.90\",\n" +
            "\t\t\"zds.livy.spark.speculation.multiplier\": \"2\",\n" +
            "\t\t\"zds.livy.spark.driver.extraClassPath\": \"avro-1.8.2.jar\",\n" +
            "\t\t\"zds.livy.spark.executor.extraClassPath\": \"avro-1.8.2.jar\",\n" +
            "\t\t\"zds.livy.spark.jars\": \"\"\n" +
            "\t},\n" +
            "\t\"zds.jdbc\": {\n" +
            "\t\t\"zds.jdbc." + NOTEBOOK_VIEW_MAX_ROW + "\": \"" + 1000 + "\",\n" +
            //"\t\t\"zds.jdbc.spark.yarn.queue\": \"auto\",\n" +
            "\t\t\"zds.jdbc.spark.yarn.queue\": \"auto\"\n" +
            //"\t\t\"zds.jdbc." + session_idle_timeout + "\": \"" + 3600 + "\"\n" +
            "\t}\n" +
            "}";

    public final static ZetaNotebook DEFAULT_CONFIG_OBJECT;
    public final static ZetaNotebook DEFAULT_DYNAMIC_CONFIG_OBJECT_VIEW;
    static {
        DEFAULT_CONFIG_OBJECT=new ZetaNotebook();
        DEFAULT_CONFIG_OBJECT.setId(DEFAULT_REFERENCE_KEY);
        DEFAULT_CONFIG_OBJECT.setTitle(DEFAULT_REFERENCE_KEY);
        DEFAULT_CONFIG_OBJECT.setContent(DEFAULT_SPARK_CONFIG);
        DEFAULT_CONFIG_OBJECT.setPath("/conf");

        DEFAULT_DYNAMIC_CONFIG_OBJECT_VIEW=new ZetaNotebook();
        DEFAULT_DYNAMIC_CONFIG_OBJECT_VIEW.setId(DEFAULT_REFERENCE_KEY);
        DEFAULT_DYNAMIC_CONFIG_OBJECT_VIEW.setTitle(DEFAULT_REFERENCE_KEY);
        String dynamicContent = DEFAULT_SPARK_CONFIG.replace(DEFAULT_LIVY_SPARK_QUEUE, "auto");
        DEFAULT_DYNAMIC_CONFIG_OBJECT_VIEW.setContent(dynamicContent);
        DEFAULT_DYNAMIC_CONFIG_OBJECT_VIEW.setPath("/conf");
    }

    public static String getDynamicSparkConf(Integer clusterId, String userName) {
        String queue = getDynamicQueue(clusterId, userName);
        if (queue == null || queue.equals(DEFAULT_LIVY_SPARK_QUEUE)) return DEFAULT_SPARK_CONFIG;
        return DEFAULT_SPARK_CONFIG.replace(DEFAULT_LIVY_SPARK_QUEUE, queue);
    }

    public static String getDynamicQueue(Integer clusterId, String userName) {
        List<String> queues = configurationManager
                .getUserQueueInfo(ConfigurationManager.Cluster.valueOfClusterId(clusterId), userName);
        if (queues != null) {
            queues = queues.stream()
                    .filter(q -> !q.equals("default") && !q.equals("hdlq-data-zeta")) // connection queue!
                    .collect(Collectors.toList());
            logger.warn("the queue: default is filtered...");
        }
        if (queues == null || queues.size() == 0) {
            logger.info("no dynamic queue got, using default");
            return null;
        }
        // dynamicConfigurationContext.setDynamicQueue(userName, clusterId, queues.get(0));
        logger.info("got dynamic queue: {} for user: {} in cluster: {}", queues.get(0),
                userName, Cluster.valueOfClusterId(clusterId));
        return queues.get(0);
    }

    @Component
    public static class InterpreterConfigurationFactory {

        public InterpreterConfiguration newInstance() {
            return new InterpreterConfiguration();
        }

        public InterpreterConfiguration newInstance(String confPath) {
            return new InterpreterConfiguration(confPath);
        }
    }

}
