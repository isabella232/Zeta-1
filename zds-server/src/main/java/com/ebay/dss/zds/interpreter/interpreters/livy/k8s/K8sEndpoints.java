package com.ebay.dss.zds.interpreter.interpreters.livy.k8s;

import com.ebay.dss.zds.common.JsonUtil;
import com.ebay.dss.zds.model.tess.TessKubeToUDNS;
import io.fabric8.kubernetes.api.model.Service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.ebay.dss.zds.cluster.env.k8s.KubernetesEnv.ANNOTATION_KUBE2UDNS;

/**
 * Created by tatian on 2020-11-20.
 */
public class K8sEndpoints {

  public final List<K8sEndpoint> endpoints;

  public K8sEndpoints(List<K8sEndpoint> endpoints) {
    this.endpoints = endpoints;
  }

  public static K8sEndpoints fromKubernetesEndpoints(io.fabric8.kubernetes.api.model.Endpoints k8sEndpoints) {
    List<K8sEndpoint> endpoints = new CopyOnWriteArrayList<>();
    k8sEndpoints.getSubsets().forEach(sub -> {
      sub.getAddresses().forEach(address -> endpoints.add(new K8sEndpoint(address.getHostname(), address.getIp())));
    });
    return new K8sEndpoints(endpoints);
  }

  public static K8sEndpoints fromTessKubeToUDNS(TessKubeToUDNS kubeToUDNS) {
    List<K8sEndpoint> endpoints = new CopyOnWriteArrayList<>();
    kubeToUDNS.getRecords().forEach(kubeToDNSRecord -> endpoints.add(new K8sEndpoint(kubeToDNSRecord.host, kubeToDNSRecord.ip)));
    return new K8sEndpoints(endpoints);
  }

  public static K8sEndpoints fromService(Service service) {
    String kube2udns = service.getMetadata()
            .getAnnotations()
            .get(ANNOTATION_KUBE2UDNS);
    TessKubeToUDNS udns = TessKubeToUDNS.fromAnnotationStr(kube2udns);
    return fromTessKubeToUDNS(udns);
  }

  public String toJson() {
    return JsonUtil.GSON.toJson(this);
  }
}
