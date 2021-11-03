package com.ebay.dss.zds.serverconfig;

import com.ebay.dss.zds.cluster.env.k8s.KubernetesEnv;
import com.ebay.dss.zds.interpreter.ConfigurationManager;
import com.ebay.dss.zds.interpreter.interpreters.livy.k8s.LivyK8sConfigurationPreLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Properties;

/**
 * Created by tatian on 2020-11-21.
 */
@Component
public class ClusterEnvConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterEnvConfiguration.class);

  private final ConfigurationManager configurationManager;

  public ClusterEnvConfiguration(ConfigurationManager configurationManager) {
    this.configurationManager = configurationManager;
  }

  @PostConstruct
  public void init() {
    try {
      Properties properties = configurationManager.getDefaultConfiguration().getProperties();
      KubernetesEnv kubernetesEnv = KubernetesEnv.get();
      if (kubernetesEnv.isActive()) {
        LivyK8sConfigurationPreLoader preLoader = new LivyK8sConfigurationPreLoader();
        preLoader.doLoad(kubernetesEnv, properties);
      } else {
        LOGGER.info("The env: KubernetesEnv is not active");
      }

    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
