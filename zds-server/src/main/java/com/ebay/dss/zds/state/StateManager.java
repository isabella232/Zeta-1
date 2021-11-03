package com.ebay.dss.zds.state;

import com.ebay.dss.zds.state.source.StateSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.Serializable;

/**
 * Created by tatian on 2020-08-19.
 */
@Component
public class StateManager {

  private static final Logger logger = LoggerFactory.getLogger(StateManager.class);

  @Autowired
  @Qualifier("StateSource")
  private StateSource stateSource;

  @Value("${zds.state.enable:#{true}}")
  private volatile boolean enabled;

  // todo:
  //1. implement the log
  //2. absolute concurrency & consistence

  public boolean storeStateSnapshot(String key, StateSnapshot stateSnapshot) {
    if (stateSnapshot == null) {
      logger.warn("The state snapshot can not be null");
      return false;
    }
    return stateSource.store(key, stateSnapshot);
  }

  public <T extends Serializable> StateSnapshot<T> findStateSnapshot(String key) {
    return stateSource.lookup(key);
  }

  public void destroyStateSnapshot(String key) {
    stateSource.remove(key);
  }

  // todo: destroy the state finally?
  public void runWithState(Runnable task, boolean removeStateAfterFinished) {
    if (task instanceof Recoverable) {
      Recoverable recoverable = null;
      try {
        recoverable = (Recoverable) task;
        storeStateSnapshot(recoverable.recoverKey(), recoverable.createSnapshot());
        task.run();
      } finally {
        if (removeStateAfterFinished) destroyStateSnapshot(recoverable.recoverKey());
      }
    } else {
      logger.info("{} is not a recoverable instance", task.getClass());
      task.run();
    }
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void enable() {
    this.enabled = true;
  }

  public void disable() {
    this.enabled = false;
  }

}
