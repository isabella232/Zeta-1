package com.ebay.dss.zds.common;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

import com.ebay.dss.zds.exception.ToolSetCheckException;
import com.ebay.dss.zds.model.Column;
import com.ebay.dss.zds.service.datamove.TableMetaService;
import com.google.common.collect.Maps;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertiesUtil {
  private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesUtil.class);

  private static final String VARIABLES_RESOURCE = "/";


  private static Map<String, String> schedulerConf = Maps.newLinkedHashMap();
  private static Map<String, String> dataMoveConf = Maps.newLinkedHashMap();
  private static Map<String, String> jupyterConf = Maps.newLinkedHashMap();

  static {
    loadSchedulerConfiguration();
  }

  public static void loadSchedulerConfiguration() {
    loadConf("/scheduler.properties", schedulerConf);
    loadConf("/datamove.properties", dataMoveConf);
    loadConf("/jupyter.properties", jupyterConf);
    LOGGER.info("Load Scheduler Properties: {}", schedulerConf);
    LOGGER.info("Load Data move Properties: {}", dataMoveConf);
    LOGGER.info("Load Jupyter Properties: {}", jupyterConf);
  }

  public static void loadConf(String fileName, Map<String, String> conf) {
    Properties properties = getProperties(fileName);
    for (Map.Entry<Object, Object> entry : properties.entrySet()) {
      Optional.ofNullable(entry.getKey()).ifPresent(
          key -> updateConfMap(conf, key.toString(), entry.getValue()));
    }
  }

  private static void updateConfMap(Map<String, String> conf, String key, Object value) {
    conf.put(key, Optional.ofNullable(value).orElse("").toString());
  }

  @Deprecated
  public static String getFilePathForVariables() {
    try {
      URL variablesUrl = PropertiesUtil.class.getResource(VARIABLES_RESOURCE);
      File file = Paths.get(variablesUrl.toURI()).toFile();
      return file.getAbsolutePath();
    } catch (URISyntaxException e) {
      e.printStackTrace();
      return null;
    }
  }

  public static String getSparkType(Column column) {
    String tdDataType = column.getDataType().toUpperCase();
    if (tdDataType.contains("TIMESTAMP")) {
      return "TIMESTAMP";
    } else if (tdDataType.contains("TIME")) {
      return "STRING";
    } else if (tdDataType.contains("NUMBER")) {
      int scale = column.getDecimalfractionaldigits() < 0 ? 38 : column.getDecimaltotaldigits();
      int precious = column.getDecimaltotaldigits() < 0 ? 2 : column.getDecimalfractionaldigits();
      return "DECIMAL(" + scale + "," + precious + ")";
    }
    return (String) getProperties("/td2SparkDataType.properties").getOrDefault(tdDataType, tdDataType);
  }

  public static boolean isScheduleSwitchOn(ScheduleSwitchKey key) {
    String value = getScheduleProperties(key.getV());
    return Objects.nonNull(value) && "on".equalsIgnoreCase(value);
  }

  public enum ScheduleSwitchKey {
    SCHEDULER("scheduler"),
    DataValidate("datavalidate"),
    DataMove("datamove"),
    DataMoveCron("datamove_scheduler"),
    DataMoveCleanFile("datamove_cleanfile"),
    MetaTableProd("metatable_prod_scheduler"),
    Email("email");
    String v;

    ScheduleSwitchKey(String v) {
      this.v = v;
    }

    public String getV() {
      return v;
    }

    public void setV(String v) {
      this.v = v;
    }
  }

  public static String getScheduleProperties(String key) {
    return schedulerConf.getOrDefault(key, "");
  }

  public static String getDatamoveProperties(String key) {
    return dataMoveConf.getOrDefault(key, "");
  }

  public static String getJupyterProperties(String key) {
    return jupyterConf.getOrDefault(key, "");
  }

  private static Properties getProperties(String propertiesFile) {
    Properties properties = new Properties();
    InputStream inStream;
    try {
      inStream = new BufferedInputStream(
          new FileInputStream("/home/b_bis/zeta-dev-suite/zds-server/conf" + propertiesFile));
    } catch (FileNotFoundException e1) {
      LOGGER.error("Can't get {} from /home/b_bis/zeta-dev-suite/zds-server/conf", propertiesFile);
      inStream = PropertiesUtil.class.getResourceAsStream(propertiesFile);
    }
    try {
      properties.load(inStream);
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (inStream != null) {
        try {
          inStream.close();
        } catch (IOException ex) {
          // ignore
        }
      }
    }
    return properties;
  }


  public static String getTemplate(String templateName) {
    String templatePath = String.format("/ddl-template/%s.template", templateName);
    InputStream inputStream = PropertiesUtil.class.getResourceAsStream(templatePath);
    try {
      return IOUtils.toString(inputStream);
    } catch (IOException e) {
      LOGGER.error("Can't open template {}", templateName);
    } finally {
      try {
        inputStream.close();
      } catch (IOException ex) {

      }
    }
    throw new ToolSetCheckException("Can't get ddl template - " + templateName);
  }

  public static Integer getInt(String key, Properties prop, int defaultValue) {
    return Integer.valueOf(prop.getProperty(key, defaultValue + ""));
  }

  public static Long getLong(String key, Properties prop, long defaultValue) {
    return Long.valueOf(prop.getProperty(key, defaultValue + ""));
  }

  public static Boolean getBoolean(String key, Properties prop, boolean defaultValue) {
    return Boolean.valueOf(prop.getProperty(key, defaultValue + ""));
  }
}
