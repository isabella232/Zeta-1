package com.ebay.dss.zds.serverconfig;

import com.ebay.dss.zds.state.source.StateSource;
import com.ebay.dss.zds.state.source.StateSourceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.util.Properties;

/**
 * Created by tatian on 2020-08-26.
 */
@Configuration
public class StateConfiguration {

  @Value("${spring.profiles.active}")
  private String profile;

  @Autowired
  private Environment environment;

  @Bean(name = "StateSource")
  public StateSource stateSource() throws Exception {
    Properties properties = envToProperties();
    return StateSourceFactory.createSource(properties);
  }

  @Bean(name = "EnvironmentProperties")
  public Properties environmentProperties() throws Exception {
    return envToProperties();
  }

  private Properties envToProperties() {
    String appFileName;
    if (profile.length() > 0) {
      appFileName = "application-" + profile + ".properties";
    } else {
      appFileName = "application.properties";
    }

    Properties properties;
    try {
      Resource resource = new ClassPathResource(appFileName);
      properties = PropertiesLoaderUtils.loadProperties(resource);
    } catch (Exception ex) {
      ex.printStackTrace();
      properties = new Properties();
    }
    return properties;
  }
}
