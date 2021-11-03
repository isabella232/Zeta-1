package com.ebay.dss.zds.interpreter;

import com.ebay.dss.zds.common.ClassUtil;
import com.ebay.dss.zds.common.Constant;
import com.ebay.dss.zds.common.EbayRealmUtils;
import com.ebay.dss.zds.common.JsonUtil;
import com.ebay.dss.zds.dao.ZetaNotebookRepository;
import com.ebay.dss.zds.exception.ApplicationBaseException;
import com.ebay.dss.zds.exception.ErrorCode;
import com.ebay.dss.zds.exception.InterpreterException;
import com.ebay.dss.zds.exception.InterpreterServiceException;
import com.ebay.dss.zds.interpreter.annotation.HermesBackend;
import com.ebay.dss.zds.interpreter.interpreters.jdbc.*;
import com.ebay.dss.zds.interpreter.interpreters.livy.ZLivySharedInterpreter;
import com.ebay.dss.zds.kerberos.KerberosContext;
import com.ebay.dss.zds.model.EbayRealm;
import com.ebay.dss.zds.model.ZetaUser;
import com.ebay.dss.zds.service.BDPHTTPService;
import com.ebay.dss.zds.service.KerberosContextService;
import com.ebay.dss.zds.kerberos.KerberosContextUser;
import com.ebay.dss.zds.service.ZetaUserService;
import com.ebay.dss.zds.model.Platform;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Maps;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.plexus.util.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import static com.ebay.dss.zds.exception.ErrorCode.INTERPRETER_CONNECT_EXCEPTION;
import static com.ebay.dss.zds.interpreter.interpreters.jdbc.Constant.*;
import static com.ebay.dss.zds.interpreter.interpreters.livy.LivyClient.LIVY_CONF_HEADER;
import static com.ebay.dss.zds.model.Platform.hermes;
import static com.ebay.dss.zds.model.ZetaStatus.INTP_SESSION_CONNECTION_ERROR;
import static com.ebay.dss.zds.service.datamove.HadoopTableOperation.ADMIN_ACCT;


/**
 * Created by tatian on 2018/6/6.
 */
@Component
public class ConfigurationManager {
  public final static Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);

  @Value("${spring.profiles.active}")
  private String profile;

  @Value("${zds.interpreter.config.path}")
  private String confPath;

  @Value("${zds.interpreter.datamove.config.path}")
  private String dataMoveConfPath;

  @Value("${zds.configuration.watch.interval:#{null}}")
  private Long watchInternal;

  @Autowired
  private Environment environment;

  private ZetaNotebookRepository zetaNotebookRepository;
  private ZetaUserService zetaUserService;
  private InterpreterConfiguration.InterpreterConfigurationFactory icFactory;
  private KerberosContextService kerberosContextService;
  private BDPHTTPService bdphttpService;

  public String getProfile() {
    return profile;
  }

  public void setProfile(String profile) {
    this.profile = profile;
  }

  public String getConfPath() {
    return confPath;
  }

  public void setConfPath(String confPath) {
    this.confPath = confPath;
  }

  @Autowired
  public ConfigurationManager(ZetaNotebookRepository zetaNotebookRepository,
                              ZetaUserService zetaUserService,
                              InterpreterConfiguration.InterpreterConfigurationFactory icFactory,
                              KerberosContextService kerberosContextService,
                              BDPHTTPService bdphttpService) {

    this.zetaNotebookRepository = zetaNotebookRepository;
    this.zetaUserService = zetaUserService;
    this.icFactory = icFactory;
    this.kerberosContextService = kerberosContextService;
    this.bdphttpService = bdphttpService;
  }

  private static final Long singletonLock = new Long(0);

  private static volatile InterpreterConfiguration _intpConf = null;

  public InterpreterConfiguration reloadConfiguration() {
    _intpConf = icFactory.newInstance(confPath);
    InterpreterConfiguration.initedConfiguration = _intpConf;
    return _intpConf;
  }

  @PostConstruct
  private void preloadConfiguration() {
    InterpreterConfiguration.setConfigPath(getConfPath());
    InterpreterConfiguration.setConfigurationManager(this);
    InterpreterConfiguration defaultConfiguration = getDefaultConfiguration();
    InterpreterConfiguration.initedConfiguration = getDefaultConfiguration();

    //for livy
    String ids = defaultConfiguration.getProperties()
            .getProperty(LIVY_CONF_HEADER + "connectionQueue.clusterIds", "");
    if (StringUtils.isNotEmpty(ids)) {
      String[] idArray = StringUtils.split(ids, ',');
      for (String s : idArray) {
        InterpreterConfiguration.setUseConnQueue(Integer.parseInt(s.trim()), true);
      }
    }

    if (watchInternal != null && watchInternal > 0 && !"dev".equals(getProfile())) {
      try {
        String parentPath = new File(confPath).getParent();
        FileAlterationObserver observer = new FileAlterationObserver(parentPath);
        observer.addListener(new FileAlterationListenerAdaptor() {

          @Override
          public void onFileCreate(File file) {
            if (file.getAbsolutePath().equals(confPath)) {
              try {
                logger.info("observed new configuration file created, reloading");
                reloadConfiguration();
                logger.info("new configuration loaded");
              } catch (Exception ex) {
                logger.error("failed to reload the new configuration");
              }
            }
          }

          @Override
          public void onFileChange(File file) {
            if (file.getAbsolutePath().equals(confPath)) {
              try {
                logger.info("observed the configuration file has been changed, reloading");
                reloadConfiguration();
                logger.info("new configuration loaded");
              } catch (Exception ex) {
                logger.error("failed to reload the new configuration");
              }
            }
          }
        });
        FileAlterationMonitor monitor = new FileAlterationMonitor(watchInternal, observer);
        monitor.start();
      } catch (Exception ex) {
        logger.error("failed to start the configuration monitor thread for path: ", ex);
      }
    }

  }

  public InterpreterConfiguration getConfigurationFromRepository(String noteId, String className, String realUser, String proxyUser, Integer clusterId, Map<String, String> prop) {
    logger.info("profile: " + getProfile() + " proxyUser: " + proxyUser);
    InterpreterConfiguration conf = icFactory.newInstance(getConfPath());
    if (!"dev".equalsIgnoreCase(getProfile())) {
      conf.init(zetaNotebookRepository, noteId, className, realUser, proxyUser, clusterId,
              Boolean.valueOf(conf.getOrDefault("zds.livy.spark.dynamic.queue", "true")), prop);
      conf.setProperty("zds.livy.username", proxyUser);
      conf.setProperty("zds.livy.clusterId", clusterId.toString());
      conf.mapClusterIdToUrl(clusterId);
    }
    return conf;
  }

  public InterpreterConfiguration getConfigurationFromRepository(String noteId, String realUser, String userName, String className, Map<String, String> prop) {
    InterpreterConfiguration conf = icFactory.newInstance(getConfPath());
    if (!"dev".equalsIgnoreCase(getProfile())) {
      conf.initForJDBC(noteId, realUser, userName, zetaNotebookRepository, className, prop);
    }
    return conf;
  }

  @Deprecated
  public InterpreterConfiguration getConfiguration(String noteId, String className, String realUser, String proxyUser, Integer clusterId, Map<String, String> prop) {
    InterpreterConfiguration conf = getConfigurationFromRepository(noteId, realUser, proxyUser, className, clusterId, prop);

    if (ZLivySharedInterpreter.class.getName().equals(className)) {
      conf.setProperty(Constant.SHARED_LIVY_CODE_TYPE, prop.get(Constant.SHARED_LIVY_CODE_TYPE));
    }

    return conf;
  }

  public Properties getConfiguration(String nt, String noteId, Map<String, String> params) {
    return prepareJdbcProperties(nt, noteId, params);
  }

  private void resolveAppliedPackages(InterpreterConfiguration interpreterConfiguration, Integer clusterId) {
    String packageList = interpreterConfiguration.getProperty(InterpreterConfiguration.APPLIED_PACKAGES_LIST);
    if (StringUtils.isNotEmpty(packageList)) {
      try {
        Map<Integer, Set<String>> packages = JsonUtil.fromJson(packageList, new TypeReference<Map<Integer, Set<String>>>() {
        });
        Set<String> filteredPackages = packages.get(clusterId);
        interpreterConfiguration.getProperties().put(InterpreterConfiguration.APPLIED_PACKAGES_LIST, filteredPackages);
        String fullPackageList = filteredPackages.stream()
                .collect(Collectors.joining(","));
        logger.info("Applied packages list resolved: " + fullPackageList);
      } catch (Exception ex) {
        logger.error("Failed to resolved packages: " + ex.toString());
      }
    } else {
      logger.info("No any package find for this configuration, the packageList is empty");
    }
  }

  public InterpreterConfiguration prepareConfiguration(String userName,
                                                       String noteId,
                                                       String interpreter,
                                                       Map<String, String> prop) {

    InterpreterConfiguration conf;
    InterpreterType.EnumType intpType = InterpreterType.fromString(interpreter);
    String interpreterClassName = intpType.getInterpreterClass().getName();
    switch (intpType) {
      case LIVY_SHARED:
      case LIVY_SPARK:
      case LIVY_SPARKR:
      case LIVY_PYSPARK:
      case LIVY_SPARKSQL:
        Integer clusterId = Integer.valueOf(prop.get("clusterId"));
        String proxyUser = prop.get("proxyUser");
        String preference = prop.get("preference");
        conf = getConfigurationFromRepository(noteId, interpreterClassName, userName, proxyUser, clusterId, prop);
        conf.setProperty("clusterId", prop.get("clusterId"));
        conf.setProperty("proxyUser", proxyUser);
        conf.setProperty("preference", preference);
        if (ZLivySharedInterpreter.class.getName().equals(interpreterClassName)) {
          conf.setProperty(Constant.SHARED_LIVY_CODE_TYPE, prop.get(Constant.SHARED_LIVY_CODE_TYPE));
        }

        // set max row
        if (!conf.getProperties().contains("zds.livy.spark.sql.maxResult")) {
          conf.setProperty("zds.livy.spark.sql.maxResult",
                  conf.getOrDefault("zds.livy." + InterpreterConfiguration.MAX_VIEW_RESULT, "1000"));
        }

        /** force using dynamic**/
        boolean forceUsingDynamic = Boolean.valueOf(conf.getOrDefault("zds.livy.spark.dynamicAllocation.force", "true"));
        if (forceUsingDynamic) {
          conf.setProperty("zds.livy.spark.dynamicAllocation.enabled", "true");
          conf.setProperty("zds.livy.spark.dynamicAllocation.minExecutors", "0");
          conf.removeProperty("zds.livy.spark.executor.instances");
          logger.info("Force using dynamic allocation for note: {}, user: {}", noteId, userName);
        }

        resolveAppliedPackages(conf, clusterId);

        break;
      case JDBC:
        conf = getConfigurationFromRepository(noteId, userName, userName, interpreterClassName, prop);
        JdbcType jdbcType = JdbcConfUtils.typeOf(prop);

        // set td charset
        final String td_charset_key = "zds.jdbc.teradata.charset";
        if (!prop.containsKey("jdbc.url.params.CHARSET") && jdbcType == JdbcType.teradata) {
          prop.put("jdbc.url.params.CHARSET", conf.getOrDefault(td_charset_key, "UTF8"));
        }

        Properties finalProp = getConfiguration(userName, noteId, prop);
        conf.merge(finalProp);
        break;
      case SSH_RESTRICTED_HDFS:
        conf = new InterpreterConfiguration();
        conf.getProperties().putAll(prop);
        break;
      case IMITATE:
        conf = new InterpreterConfiguration();
        conf.getProperties().putAll(prop);
        break;
      default:
        throw new InterpreterServiceException(INTERPRETER_CONNECT_EXCEPTION,
                INTP_SESSION_CONNECTION_ERROR, noteId, "Unknown interpreter type: " + interpreter);
    }

    return conf;
  }

  private Properties prepareJdbcProperties(String nt, String noteId, Map<String, String> params) {
    IJdbcConf conf;
    try {
      conf = JdbcConfUtils.of(nt, noteId, params);
      conf.setNt(nt);
      conf.setNotebookId(noteId);
    } catch (IllegalArgumentException e) {
      throw new InterpreterException("Not supported jdbc type");
    }

    switch (conf.getJdbcType()) {
      case teradata:
        conf = teradataPostConfigure(nt, conf);
        break;
      case kylin:
        conf = kylinPostConfigure(conf);
        break;
      case hive:
        conf = hivePostConfigure(conf);
        break;
      default:
        throw new InterpreterException("Not supported jdbc type");
    }

    return JdbcConfUtils.wrap(conf);
  }

  String getDecryptSecret(String userName, String secretName) {
    ZetaUser user = zetaUserService.getUser(userName);
    String getMethodName = "get" + StringUtils.capitalize(secretName);
    String secret = null;
    try {
      Method getMethod = user.getClass().getMethod(getMethodName);
      secret = (String) getMethod.invoke(user);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      logger.error("get secret error: ", e);
    }
    if (StringUtils.isBlank(secret)) {
      throw new InterpreterException("Password is not set");
    }
    return secret;
  }

  @NotNull
  public Map<String, String> getUserPreferenceMap(ZetaUser zetaUser) {
    String preference = zetaUser.getPreference();
    Map<String, String> preferenceMap;
    if (preference != null && preference.length() > 0) {
      try {
        preferenceMap = JsonUtil.GSON.fromJson(preference, new TypeToken<Map<String, String>>() {
        }.getType());
      } catch (Exception ex) {
        logger.error("Error when parse user: {} preference map, error: {}, preference: {}",
                zetaUser.getNt(), ExceptionUtils.getFullStackTrace(ex), preference);
        preferenceMap = new HashMap<>();
      }
    } else {
      logger.warn("Empty user preference map, user: {}", zetaUser.getNt());
      preferenceMap = new HashMap<>();
    }
    return preferenceMap;
  }

  IJdbcConf teradataPostConfigure(String userName, IJdbcConf conf) {
    conf.setUser(userName);
    conf.setPassword(getDecryptSecret(userName, "tdPass"));
    // Teradata way of building url of specified port and database
    int port = conf.getPort();
    if (port > 0 && port < 65535) {
      conf.setPort(0);
      conf.putUrlParam("DBS_PORT", String.valueOf(port));
    }
    String db = conf.getDatabase();
    if (StringUtils.isNotBlank(db)) {
      conf.setDatabase(null);
      conf.putUrlParam("DATABASE", db);
    }
    conf.putUrlParam("REDRIVE", "4");
    conf.putUrlParam("TCP", "KEEPALIVE");
    return conf;
  }

  IJdbcConf kylinPostConfigure(IJdbcConf conf) {
    if (conf.isSsl()) {
      conf.putProperty("ssl", "true");
    }
    conf.putProperty("user", conf.getUser());
    conf.putProperty("password", conf.getPassword());
    return conf;
  }

  private static final String HIVE_IMPERSONATE_KITE_KEY = "zds.server.impersonate.kite";
  private static final String HIVE_IMPERSONATE_KITE_DEFAULT = "true";
  private static final String HIVE_IMPERSONATE_PRINCIPAL_KEY = "zds.server.impersonate.principal";
  private static final String HIVE_IMPERSONATE_PRINCIPAL_DEFAULT = "b_zeta_devsuite@PROD.EBAY.COM";
  private static final String HIVE_IMPERSONATE_TICKET_CACHE_KEY = "zds.server.impersonate.ticket-cache";
  private static final String HIVE_IMPERSONATE_TICKET_CACHE_DEFAULT = "/tmp/krb5cc_b_zeta_devsuite";

  IJdbcConf hivePostConfigure(final IJdbcConf conf) {
    String remotePrincipal = conf.getProperty("hive.server2.remote.principal");
    EbayRealm realm = EbayRealmUtils.realmOf(remotePrincipal);
    if (Objects.isNull(realm)) {
      throw new ApplicationBaseException(ErrorCode.UNKNOWN_HIVE_REALM);
    }
    String fileLoc, principal;
    KerberosContextUser contextUser;
    boolean kiteEnabled = BooleanUtils.toBoolean(
            System.getProperty(HIVE_IMPERSONATE_KITE_KEY, HIVE_IMPERSONATE_KITE_DEFAULT)
    );
    if (kiteEnabled) {
      fileLoc = System.getProperty(HIVE_IMPERSONATE_TICKET_CACHE_KEY, HIVE_IMPERSONATE_TICKET_CACHE_DEFAULT);
      principal = System.getProperty(HIVE_IMPERSONATE_PRINCIPAL_KEY, HIVE_IMPERSONATE_PRINCIPAL_DEFAULT);
      contextUser = new KerberosContextUser(principal, fileLoc, true);
    } else {
      fileLoc = System.getProperty("hive.server2.rno.impersonate.secret-file");
      principal = System.getProperty("hive.server2.rno.impersonate.principal");
      contextUser = new KerberosContextUser(principal, fileLoc);
    }

    conf.putUrlParam("hive.server2.proxy.user", conf.getUser());
    return hiveDriverPostConfigure(conf, contextUser);
  }

  IJdbcConf hiveDriverPostConfigure(final IJdbcConf conf,
                                    KerberosContextUser contextUser) {
    conf.putUrlParam("principal", conf.getProperty("hive.server2.remote.principal"));
    KerberosContext kerberosContext;
    try {
      kerberosContext = kerberosContextService.get(contextUser);
    } catch (Exception e) {
      logger.error("kerberos login error", e);
      throw new RuntimeException(e);
    }
    conf.setDataSourceSupplier(() -> KerberosDataSource.newInstance(conf, kerberosContext));

    /** put the context in the properties **/
    conf.put("KerberosContext", kerberosContext);
    return conf;
  }

  public InterpreterConfiguration getDefaultConfiguration() {
    if (_intpConf == null) {
      synchronized (singletonLock) {
        _intpConf = icFactory.newInstance(getConfPath());
      }
    }
    return _intpConf;
  }

  // todo: useSSL
  public InterpreterConfiguration getDataMoveConfiguration(String proxyUser, Platform platform) {
    InterpreterConfiguration conf;
    switch (platform) {
      case hermes:
      case hermesrno:
        InterpreterConfiguration tmpConf = icFactory.newInstance(getConfPath());
        tmpConf.mapClusterIdToUrl(hermes.getId());

        Map<String, String> prop = Maps.newLinkedHashMap();

//        prop.put("clusterId", hermes.getId() + "");
//        InterpreterConfiguration tmpConf = icFactory.newInstance(getConfPath());
//        resolveHermesParameters(proxyUser, tmpConf, prop);
//        String noteId = InterpreterManager.genOnetimeNoteId(proxyUser, platform.getId());
//        Properties properties = getConfiguration(proxyUser, noteId, prop);
//        conf = tmpConf;
//        conf.merge(properties);

        prop.put(HOST_KEY, tmpConf.getProperty(HOST_KEY));
        prop.put(USER_KEY, proxyUser);
        prop.put(DATABASE_KEY, "access_views");
        prop.put("jdbc.props.hive.server2.remote.principal",
            tmpConf.getProperty("jdbc.props.hive.server2.remote.principal"));
        if (ADMIN_ACCT.equalsIgnoreCase(proxyUser)) {
          // test queue: hdmi-reserved-test
          prop.put("jdbc.props.spark.yarn.queue", "hdmi-default");
        }
        prop.put(PORT_KEY, "10001");
        prop.put(JDBC_TYPE_KEY, "carmel");
        prop.put(SSL_KEY, "true");
        String noteId = InterpreterManager.genOnetimeNoteId(proxyUser, platform.getId());
        Properties properties = getConfiguration(proxyUser, noteId, prop);
        conf = new InterpreterConfiguration(properties);
        break;
      default:
        conf = new InterpreterConfiguration(dataMoveConfPath);
        conf.setProperty("zds.livy.username", proxyUser);
        // add this to track cluster id
        conf.setProperty("zds.livy.clusterId", Integer.toString(platform.getId()));
        conf.mapClusterIdToUrl(platform.getId());
        break;
    }
    return conf;

  }

  public List<String> getUserQueueInfo(String clusterName, String userName) {
    return bdphttpService.getUserQueueInfo(clusterName, userName);
  }

  public enum Cluster {
    herculeslvs(10), herculessublvs(11), areslvs(2), apollophx(3), apollorno(14),

    @HermesBackend
    hermesrno(16),
    @HermesBackend
    hermeslvs(21);

    private int id;
    private static Map<Integer, Cluster> map = new HashMap<>();
    private static Map<Integer, Set<Class<? extends Annotation>>> clusterAnnos = new HashMap<>();

    Cluster(int id) {
      this.id = id;
    }

    public int getId() {
      return id;
    }

    static {
      Map<String, Cluster> nameAndCluster = new HashMap<>();
      for (Cluster cluster : Cluster.values()) {
        map.put(cluster.id, cluster);
        nameAndCluster.put(cluster.name(), cluster);
      }

      Arrays.stream(Cluster.class.getDeclaredFields()).forEach(field -> {
        Cluster cluster = nameAndCluster.get(field.getName());
        if (cluster != null) {
          Set<Class<? extends Annotation>> annos = clusterAnnos.get(cluster.id);
          if (annos == null) {
            annos = new HashSet<>();
            for (Annotation anno : field.getAnnotations()) {
              annos.add(anno.annotationType());
            }
            clusterAnnos.put(cluster.id, annos);
          } else {
            for (Annotation anno : field.getAnnotations()) {
              annos.add(anno.getClass());
            }
          }
        }
      });
    }

    public static String valueOfClusterId(int id) {
      return Optional.ofNullable(map.get(id)).map(Cluster::name).orElse(null);
    }

    public static Cluster fromClusterId(int id) throws Exception {
      return Optional.ofNullable(map.get(id)).orElseThrow(
              () -> new Exception("can't identify the cluster id: " + id));
    }

    public static boolean hasAnnotation(Integer id, Class<? extends Annotation> annotationClass) {
      return Optional.ofNullable(clusterAnnos.get(id)).map(
              annos -> annos.contains(annotationClass)).orElse(false);
    }

  }

  public Environment getEnvironment() {
    return environment;
  }

  public static class YarnTags {

    public static String mergeUnique(String yarnTags, String newTag) {
      List<String> tags = Arrays.stream(yarnTags.split(",")).map(String::trim).collect(Collectors.toList());
      if (!tags.contains(newTag)) {
        tags.add(newTag);
      }
      return String.join(",", tags);
    }

    public static String add(String yarnTags, String newTag) {
      String tags = yarnTags;
      if (StringUtils.isNotEmpty(tags)) {
        tags = tags + "," + newTag;
      } else {
        tags = newTag;
      }
      return tags;
    }

  }

}
