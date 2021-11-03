package com.ebay.dss.zds.cluster.env.k8s.listener;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;

/**
 * Created by tatian on 2020-11-20.
 */
public interface KubernetesEventListener {

  void eventReceived(Watcher.Action action, Service service);

  void eventReceived(Watcher.Action action, StatefulSet statefulSet);

  void onClose(KubernetesClientException exception);

  default Watcher<Service> serviceWatcher() {
    return new Watcher<Service>() {
      @Override
      public void eventReceived(Action action, Service service) {
        this.eventReceived(action, service);
      }

      @Override
      public void onClose(KubernetesClientException e) {
        this.onClose(e);
      }
    };
  }

  default Watcher<StatefulSet> statefulSetWatcher() {
    return new Watcher<StatefulSet>() {
      @Override
      public void eventReceived(Action action, StatefulSet statefulSet) {
        this.eventReceived(action, statefulSet);
      }

      @Override
      public void onClose(KubernetesClientException e) {
        this.onClose(e);
      }
    };
  }
}
