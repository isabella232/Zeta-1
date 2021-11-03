package com.ebay.dss.zds.interpreter.interpreters.livy;

import com.ebay.dss.zds.exception.ConfigurationNotFoundException;
import com.ebay.dss.zds.interpreter.InterpreterConfiguration;
import joptsimple.internal.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Created by tatian on 2019-10-16.
 */
public class LivyClientBuilder {

  private static final Logger LOGGER = LoggerFactory.getLogger(LivyClientBuilder.class);

  Properties _prop = new Properties();

  public LivyClientBuilder withProperties(Properties prop) {
    if (_prop.size() > 0) {
      LOGGER.warn("Overwrite existed properties: " + _prop.toString());
    }
    _prop = prop;
    return this;
  }

  public LivyClientBuilder withInterpreterConfiguration(InterpreterConfiguration conf) {
    if (_prop.size() > 0) {
      LOGGER.warn("Overwrite existed properties: " + _prop.toString());
    }
    Properties prop = conf.getProperties();
    _prop = prop;
    return this;
  }

  public LivyClientBuilder withSessionKind(String kind) {
    _prop.put(LivyClient.LIVY_CONF_SESSION_KIND, kind);
    return this;
  }

  public LivyClientBuilder withURL(String url) {
    _prop.put(LivyClient.LIVY_CONF_URL, url);
    return this;
  }

  public LivyClientBuilder withClusterId(int clusterId, boolean withDefaultConf)
          throws ConfigurationNotFoundException {
    if (withDefaultConf) {
      InterpreterConfiguration interpreterConfiguration = InterpreterConfiguration.getInitedConfiguration();
      if (interpreterConfiguration == null) {
        throw new ConfigurationNotFoundException("The default configuration is not found!");
      }
      withInterpreterConfiguration(interpreterConfiguration);
      LOGGER.info("LivyClient build with inited InterpreterConfiguration");
    }
    mapClusterIdToUrl(_prop, clusterId);
    return this;
  }

  public LivyClientBuilder withAuthentication() {
    _prop.put(LivyClient.LIVY_AUTHENTICATION_ENABLED, "true");
    return this;
  }

  public LivyClientBuilder withoutAuthentication() {
    _prop.put(LivyClient.LIVY_AUTHENTICATION_ENABLED, "false");
    return this;
  }

  public LivyClientBuilder addInWhiteList(String url) {
    String whiteList = _prop.getProperty(LivyClient.LIVY_CONF_AUTH_WHITELIST);
    List<String> lists = Arrays.asList(whiteList.split(","));
    lists.add(url);
    _prop.setProperty(LivyClient.LIVY_CONF_AUTH_WHITELIST, Strings.join(lists, ","));
    return this;
  }

  public LivyClientBuilder removeFromWhiteList(String url) {
    String whiteList = _prop.getProperty(LivyClient.LIVY_CONF_AUTH_WHITELIST);
    List<String> lists = Arrays.asList(whiteList.split(","));
    lists.remove(url);
    _prop.setProperty(LivyClient.LIVY_CONF_AUTH_WHITELIST, Strings.join(lists, ","));
    return this;
  }

  public LivyClient build() {
    return new LivyClient(_prop);
  }

  public Properties toProperties() {
    return _prop;
  }

  public static Properties mapClusterIdToUrl(Properties prop, int clusterId) {
    List<String> list = prop.entrySet().stream().filter(kv -> {
      String key = (String) kv.getKey();
      if (key.contains("zds.livy.url.cluster_")) {
        String id = key.substring(key.lastIndexOf("_") + 1);
        return Integer.valueOf(id).equals(clusterId);
      } else return false;
    }).map(kv -> (String) kv.getValue()).collect(Collectors.toList());
    if (list.size() > 0) {
      prop.setProperty(LivyClient.LIVY_CONF_URL, list.get(0));
    } else {
      LOGGER.warn("Can't find any zds.livy.url.cluster_ configured");
    }
    return prop;
  }
}
