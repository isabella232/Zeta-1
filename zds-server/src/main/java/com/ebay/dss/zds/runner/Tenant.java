package com.ebay.dss.zds.runner;

import com.ebay.dss.zds.interpreter.InterpreterType;
import com.ebay.dss.zds.websocket.notebook.dto.ConnectionReq;
import com.ebay.dss.zds.websocket.notebook.dto.ExecuteCodeReq;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by tatian on 2020-08-27.
 */
public class Tenant {

  public static final String EXECUTION_TENANT_PREFIX = "zds.execution.tenant.";

  public static final String CORE_POOL_SIZE = "corePoolSize";
  public static final String MAX_POOL_SIZE = "maxPoolSize";
  public static final String QUEUE_CAPACITY = "queueCapacity";
  public static final String KEEP_ALIVE_SECONDS = "keepAliveSeconds";
  public static final String TASK_START_TIMEOUT = "taskStartTimeout";

  public static final String EXECUTION_TENANT_FALLBACKS = "fallbacks";

  private String name;
  private Properties prop;
  private List<String> fallbackTenants;

  public Tenant(String name, Properties prop) {
    this.name = name;
    this.prop = prop;
  }

  public List<String> getFallbackTenants(Properties prop) {
    if (fallbackTenants == null) {
      String fallbacks = prop.getProperty(getFallbackKey(), "");
      fallbackTenants = Arrays.stream(fallbacks.split(","))
              .map(String::trim)
              .filter(s -> s.length() > 0)
              .collect(Collectors.toList());
    }
    return fallbackTenants;
  }

  public void setFallbackTenants(String fallbacks) {
    setFallbackTenants(Arrays.stream(fallbacks.split(","))
            .map(String::trim)
            .filter(s -> s.length() > 0)
            .collect(Collectors.toList()));
  }

  public Tenant setTaskStartTimeout(int num) {
    this.prop.setProperty(getFullTenantName(name) + ".taskStartTimeout" , num + "");
    return this;
  }

  private void syncFallbacks() {
    this.prop.setProperty(getFallbackKey(), String.join(",", fallbackTenants));
  }

  public void setFallbackTenants(List<String> fallbacks) {
    fallbackTenants = fallbacks;
    syncFallbacks();
  }

  public void addFallbackTenants(String fallback) {
    if (fallbackTenants == null) {
      fallbackTenants = new ArrayList<>();
    }
    fallbackTenants.add(fallback);
    syncFallbacks();
  }

  public String getFallbackKey() {
    return getFullTenantName(name) + "." + EXECUTION_TENANT_FALLBACKS;
  }

  public static String getFullTenantName(String shortName) {
    return EXECUTION_TENANT_PREFIX + shortName;
  }

  public static Properties lookupProperties(String shortName, Properties properties) {
    Properties prop = new Properties();
    properties.keySet().stream().map(Object::toString)
            .filter(s -> s.startsWith(getFullTenantName(shortName)))
            .forEach(p -> prop.put(p, properties.getProperty(p)));
    return prop;
  }

  public static Map<String, Tenant> fromProperties(Properties properties) {
    Map<String, Tenant> tenantMap = new HashMap<>();
    properties.keySet().stream().map(Object::toString)
            .filter(s -> s.startsWith(EXECUTION_TENANT_PREFIX))
            .forEach(s -> {
              String name = extractTenantName(s);
              if (StringUtils.isNotEmpty(name)) {
                if (tenantMap.containsKey(name)) {
                  Tenant tenant = tenantMap.get(name);
                  tenant.prop.put(s, properties.getProperty(s));
                } else {
                  Properties prop = new Properties();
                  prop.put(s, properties.getProperty(s));
                  tenantMap.put(name, new Tenant(name, prop));
                }
              }
            });
    return tenantMap;
  }

  public static String extractTenantName(String str) {
    if (str.startsWith(EXECUTION_TENANT_PREFIX)) {
      String name_tail = str.substring(EXECUTION_TENANT_PREFIX.length());
      return name_tail.substring(0, name_tail.indexOf("."));
    } else {
      return null;
    }
  }

  public static String getNameByReq(ExecuteCodeReq req) {
    return InterpreterType.fromString(req.getInterpreter()).getName();
  }

  public static String getNameByReq(ConnectionReq req) {
    return InterpreterType.fromString(req.getInterpreter()).getName();
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Properties getProp() {
    return prop;
  }

  public void setProp(Properties prop) {
    this.prop = prop;
  }

  public List<String> fallbackTenants() {

    return this.getFallbackTenants(prop);
  }

  public static class TenantBuilder {

    private Properties properties;
    private String name;

    public TenantBuilder(String name) {
      this.name = name;
      this.properties = new Properties();
    }

    public String getFullParamName(String param) {
      checkName();
      return EXECUTION_TENANT_PREFIX + name + "." + param;
    }

    public TenantBuilder name(String name) {
      this.name = name;
      return this;
    }

    public TenantBuilder corePoolSize(int num) {
      properties.put(getFullParamName(CORE_POOL_SIZE), num + "");
      return this;
    }

    public TenantBuilder maxPoolSize(int num) {
      properties.put(getFullParamName(MAX_POOL_SIZE), num + "");
      return this;
    }

    public TenantBuilder queueCapacity(int num) {
      properties.put(getFullParamName(QUEUE_CAPACITY), num + "");
      return this;
    }

    public TenantBuilder keepAliveSeconds(int num) {
      properties.put(getFullParamName(KEEP_ALIVE_SECONDS), num + "");
      return this;
    }

    public TenantBuilder taskStartTimeout(int num) {
      properties.put(getFullParamName(TASK_START_TIMEOUT), num + "");
      return this;
    }

    public TenantBuilder fallbacks(String fallbacks) {
      properties.put(getFullParamName(EXECUTION_TENANT_FALLBACKS), fallbacks);
      return this;
    }

    public Tenant build() {
      return new Tenant(name, properties);
    }

    public void checkName() {
      if (StringUtils.isEmpty(name)) throw new IllegalArgumentException("The name is not set");
    }

  }
}
