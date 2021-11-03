package com.ebay.dss.zds.cluster.env.k8s;

import com.ebay.dss.zds.cluster.env.ClusterEnv;
import com.ebay.dss.zds.cluster.env.k8s.listener.KubernetesEventListener;
import com.ebay.dss.zds.cluster.exception.EnvNotReadyException;
import com.ebay.dss.zds.interpreter.interpreters.livy.k8s.K8sEndpoints;
import com.ebay.dss.zds.model.tess.TessKubeToUDNS;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.util.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by tatian on 2020-11-19.
 */
public class KubernetesEnv implements ClusterEnv {

  private final static Logger logger = LoggerFactory.getLogger(KubernetesEnv.class);

  public static final String KUBERNETES_SECRETS_ROOT = "/var/run/secrets/kubernetes.io";
  public static final String ANNOTATION_KUBE2UDNS = "network.tess.io/kube2udns";
  public static final String LABEL_APP = "app";
  public static final String NAMESPACE_POD_PATH = System.getenv().getOrDefault("KUBERNETES_NAMESPACE_POD_PATH",
          KUBERNETES_SECRETS_ROOT + "/serviceaccount/namespace");
  private static final boolean _active = new File(KUBERNETES_SECRETS_ROOT).exists();
  private static final String NAMESPACE = currentNamespaceNullIfInactive();
  private static final Optional<DefaultKubernetesClient> listenerClient = newClient();
  private static final Optional<DefaultKubernetesClient> requestClient = newClient();

  private static final KubernetesEnv singleton = new KubernetesEnv();
  private static final KubernetesEnvCacheLayer cacheLayer = new KubernetesEnvCacheLayer(singleton);

  private KubernetesEnv() {

  }

  public static KubernetesEnv get() {
    return singleton;
  }

  public KubernetesEnvCacheLayer cacheLayer() {
    return cacheLayer;
  }

  public static class KubernetesEnvCacheLayer {

    private static final ConcurrentHashMap<String, K8sEndpoints> appToK8sEndpoints = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, K8sEndpoints> serviceToK8sEndpoints = new ConcurrentHashMap<>();

    private KubernetesEnv env;

    private KubernetesEnvCacheLayer(KubernetesEnv env) {
      this.env = env;
    }

    private final KubernetesEventListener listener = new KubernetesEventListener() {
      public void eventReceived(Watcher.Action action, Service service) {
        // todo: make sure get the right name
        String name = service.getMetadata().getName();
        logger.error("Kubernetes event: {} for service: {}", action, name);
        if (action == Watcher.Action.MODIFIED || action == Watcher.Action.ADDED) {
          K8sEndpoints k8sEndpoints = K8sEndpoints.fromService(service);
          serviceToK8sEndpoints.put(name, k8sEndpoints);
          logger.error("Successfully update endpoints for service: {}, endpoints: {}", name, k8sEndpoints.toJson());
        } else {
          logger.error("Got: {} for service: {}, clear the endpoints", action, name);
          Optional.ofNullable(serviceToK8sEndpoints.get(name))
                  .ifPresent(k8sEndpoints -> k8sEndpoints.endpoints.clear());
        }
      }

      public void eventReceived(Watcher.Action action, StatefulSet statefulSet) {
        // todo: make sure get the right name
        String name = statefulSet.getMetadata().getName();
        logger.error("Kubernetes event: {} for statefulset: {}", action, name);
        if (action == Watcher.Action.MODIFIED || action == Watcher.Action.ADDED) {
          Endpoints endpoints = env.getAppEndpoints(name);
          K8sEndpoints k8sEndpoints = K8sEndpoints.fromKubernetesEndpoints(endpoints);
          serviceToK8sEndpoints.put(name, k8sEndpoints);
          logger.error("Successfully update endpoints for statefulset: {}, endpoints: {}", name, k8sEndpoints.toJson());
        } else {
          logger.error("Got: {} for statefulset: {}, clear the endpoints", action, name);
          Optional.ofNullable(appToK8sEndpoints.get(name))
                  .ifPresent(k8sEndpoints -> k8sEndpoints.endpoints.clear());
        }
      }

      public void onClose(KubernetesClientException exception) {
        logger.error(exception.getMessage());
      }
    };

    public K8sEndpoints getK8sEndpointsByStatefulSetName(String appName) {
      K8sEndpoints k8sEndpoints;
      k8sEndpoints = appToK8sEndpoints.get(appName);
      if (k8sEndpoints != null) {
        return k8sEndpoints;
      } else {
        Endpoints endpoints = env.getAppEndpoints(appName);
        if (endpoints == null) {
          logger.info("Failed to get endpoints by app: {} directly from k8s api", appName);
          return null;
        } else {
          logger.info("Successfully find endpoints from k8s api by app: {}", appName);
          k8sEndpoints = K8sEndpoints.fromKubernetesEndpoints(endpoints);
          appToK8sEndpoints.put(appName, k8sEndpoints);
          listenToStatefulSet(appName);
          return k8sEndpoints;
        }
      }
    }

    public K8sEndpoints getK8sEndpointsByServiceName(String serviceName) {
      K8sEndpoints k8sEndpoints;
      k8sEndpoints = serviceToK8sEndpoints.get(serviceName);
      if (k8sEndpoints != null) {
        return k8sEndpoints;
      } else {
        TessKubeToUDNS tessKubeToUDNS = env.geServicePodsHost(serviceName);
        if (tessKubeToUDNS == null) {
          logger.info("Failed to get endpoints by service: {} directly from k8s api", serviceName);
          return null;
        } else {
          logger.info("Successfully find endpoints from k8s api by service: {}", serviceName);
          k8sEndpoints = K8sEndpoints.fromTessKubeToUDNS(tessKubeToUDNS);
          serviceToK8sEndpoints.put(serviceName, k8sEndpoints);
          listenToService(serviceName);
          return k8sEndpoints;
        }
      }
    }

    public void listenToService(String serviceName) {
      env.registerServiceListener(serviceName, listener);
    }

    public void listenToStatefulSet(String statefulsetName) {
      env.registerStatefulSetListener(statefulsetName, listener);
    }

  }

  public TessKubeToUDNS getServicePodsHost(String namespace, String serviceName) {
    if (!_active) throw new EnvNotReadyException();
    return requestClient.map(client -> {
      String kube2udns = client.inNamespace(namespace)
              .services()
              .withName(serviceName)
              .get()
              .getMetadata()
              .getAnnotations()
              .get(ANNOTATION_KUBE2UDNS);
      return TessKubeToUDNS.fromAnnotationStr(kube2udns);
    }).orElse(null);
  }

  public String getServiceApp(String namespace, String serviceName) {
    if (!_active) throw new EnvNotReadyException();
    return requestClient.map(client ->
            client.inNamespace(namespace)
                    .services()
                    .withName(serviceName)
                    .get()
                    .getMetadata()
                    .getLabels()
                    .get(LABEL_APP)
    ).orElse(null);
  }

  public ObjectMeta getServiceMetadata(String namespace, String serviceName) {
    if (!_active) throw new EnvNotReadyException();
    return requestClient.map(client -> client.inNamespace(namespace)
            .services()
            .withName(serviceName)
            .get()
            .getMetadata()
    ).orElse(null);
  }

  public TessKubeToUDNS geServicePodsHost(String serviceName) {
    return getServicePodsHost(currentNamespace(), serviceName);
  }

  private static String lookForCurrentNamespace() {
    try {
      return new String(Files.readAllBytes(new File(NAMESPACE_POD_PATH).toPath())).trim();
    } catch (Exception ex) {
      logger.error(ExceptionUtils.getFullStackTrace(ex));
      return null;
    }
  }

  public static String currentNamespaceNullIfInactive() {
    if (!_active) {
      return null;
    };
    return currentNamespace();
  }

  public static String currentNamespace() {
    if (!_active) throw new EnvNotReadyException();
    if (StringUtils.isNotEmpty(NAMESPACE)) {
      return NAMESPACE;
    } else return lookForCurrentNamespace();
  }

  public static Optional<DefaultKubernetesClient> newClient() {
    return Optional.ofNullable(_active ? new DefaultKubernetesClient() : null);
  }

  public void registerServiceListener(String serviceName, KubernetesEventListener listener) {
    if (!_active) throw new EnvNotReadyException();
    listenerClient.ifPresent(client -> {
      NamespacedKubernetesClient ns = client.inNamespace(NAMESPACE);
      ns.services().withName(serviceName).watch(listener.serviceWatcher());
      logger.info("K8s Service: {} listener registered");
    });
  }

  public void registerStatefulSetListener(String statefulSetName, KubernetesEventListener listener) {
    if (!_active) throw new EnvNotReadyException();
    listenerClient.ifPresent(client -> {
      NamespacedKubernetesClient ns = client.inNamespace(NAMESPACE);
      ns.apps().statefulSets().withName(statefulSetName).watch(listener.statefulSetWatcher());
      logger.info("K8s StatefulSet: {} listener registered");
    });
  }

  public Endpoints getAppEndpoints(String appName) {
    if (!_active) throw new EnvNotReadyException();
    return requestClient.map(client -> {
      List<Endpoints> endpoints = client.inNamespace(NAMESPACE)
              .endpoints()
              .withLabel(LABEL_APP, appName)
              .list()
              .getItems();
      if (endpoints == null || endpoints.size() == 0) {
        return null;
      } else return endpoints.get(0);
    }).orElse(null);
  }

  public boolean isActive() {
    return _active;
  }

}
