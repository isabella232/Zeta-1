package com.ebay.dss.zds.interpreter.interpreters.livy.k8s;

import com.ebay.dss.zds.cluster.env.ClusterConfigurationLoader;
import com.ebay.dss.zds.cluster.env.k8s.KubernetesEnv;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Created by tatian on 2020-11-21.
 */
public class LivyK8sConfigurationPreLoader implements ClusterConfigurationLoader<KubernetesEnv> {

  @Override
  public void doLoad(KubernetesEnv kubernetesEnv, Properties properties) {
    List<KubernetesUrls> list = new ArrayList<>();
    properties.keySet()
            .stream()
            .map(Object::toString)
            .filter(key -> key.startsWith("zds.livy.url"))
            .map(key -> properties.getProperty(key))
            .forEach(urls -> Arrays.stream(urls.split(","))
                    .filter(url -> url.startsWith(KubernetesUrls.PREFIX))
                    .forEach(url -> list.add(KubernetesUrls.fromString(url))));

    list.stream().forEach(url -> {
      if (StringUtils.isNotEmpty(url.getService())) {
        kubernetesEnv.cacheLayer().getK8sEndpointsByServiceName(url.getService());
      }
      if (StringUtils.isNotEmpty(url.getStatefulSet())) {
        kubernetesEnv.cacheLayer().getK8sEndpointsByStatefulSetName(url.getStatefulSet());
      }
    });

  }
}
