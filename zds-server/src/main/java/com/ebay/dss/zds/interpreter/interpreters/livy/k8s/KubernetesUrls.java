package com.ebay.dss.zds.interpreter.interpreters.livy.k8s;

import com.ebay.dss.zds.cluster.env.k8s.KubernetesEnv;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by tatian on 2020-11-20.
 */
public class KubernetesUrls {

  private final static Logger logger = LoggerFactory.getLogger(KubernetesUrls.class);

  public static final String SCHEME = "k8s";
  public static final String PREFIX = "k8s://k8s-resource/";

  public static final String SERVICE = "service";
  public static final String STATEFULSET = "statefulset";
  public static final String NAMESPACE = "namespace";
  public static final String PROTOCOL = "protocol";

  private String service;
  private String statefulSet;
  private String namespace;
  private String protocol = "http";
  private int port;

  public KubernetesUrls(String service,
                        String statefulSet,
                        String namespace,
                        String protocol,
                        int port) {
    this.service = service;
    this.statefulSet = statefulSet;
    this.namespace = namespace;
    this.protocol = protocol;
    this.port = port;
  }

  private KubernetesUrls() {
  }

  public List<String> getPodEndpoints() {
    try {
      KubernetesEnv env = KubernetesEnv.get();
      if (env.isActive()) {
        K8sEndpoints k8sEndpoints = null;
        if (StringUtils.isNotEmpty(service)) {
          k8sEndpoints = env.cacheLayer().getK8sEndpointsByServiceName(service);
        } else if (StringUtils.isNotEmpty(statefulSet)) {
          k8sEndpoints = env.cacheLayer().getK8sEndpointsByStatefulSetName(statefulSet);
        }
        if (k8sEndpoints != null && k8sEndpoints.endpoints.size() > 0) {
          return k8sEndpoints.endpoints.stream()
                  .map(k8sEndpoint -> protocol + "://" + k8sEndpoint.ip + ":" + port)
                  .collect(Collectors.toList());
        }
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      return null;
    }
    return null;
  }

  public static KubernetesUrls fromString(String url) {
    assert url.startsWith(PREFIX);
    String[] raw = url.substring(PREFIX.length()).split(":");
    assert raw.length == 2;
    String body = raw[0];
    KubernetesUrls kubernetesUrls = new KubernetesUrls();
    kubernetesUrls.port = Integer.valueOf(raw[1]);
    String[] segments = body.split("/");
    for (String segment : segments) {
      String[] resource = segment.split("@");
      switch (resource[0]) {
        case SERVICE:
          kubernetesUrls.service = resource[1].trim();
          break;
        case STATEFULSET:
          kubernetesUrls.statefulSet = resource[1].trim();
          break;
        case NAMESPACE:
          kubernetesUrls.namespace = resource[1].trim();
          break;
        case PROTOCOL:
          kubernetesUrls.protocol = resource[1].trim();
          break;
        default:
          break;
      }
    }
    return kubernetesUrls;
  }

  public String getService() {
    return service;
  }

  public String getStatefulSet() {
    return statefulSet;
  }

  public static void main(String[] args) throws Exception {
    URI uri = new URI("k8s://k8s-resource/statefulset@livy-apollorno/namespace@zeta-prod-ns:7050");
    System.out.println(uri.getScheme());
  }

}
