package com.ebay.dss.zds.state;

import org.apache.commons.lang.StringUtils;

import java.io.Serializable;

/**
 * Created by tatian on 2020-08-19.
 */
public class StateSnapshot<T extends Serializable> implements Serializable {

  private T data;
  private transient StateManager stateManager;
  private transient String key;

  public StateSnapshot(T data) {
    this.data = data;
  }

  public T unwrap() {
    return data;
  }

  protected void setStateManager(StateManager stateManager) {
    this.stateManager = stateManager;
  }

  protected void setKey(String key) {
    this.key = key;
  }

  public boolean destroy() {
    if (this.stateManager != null && StringUtils.isNotEmpty(this.key)) {
      this.stateManager.destroyStateSnapshot(key);
      return true;
    } else {
      return false;
    }
  }

  public static final StateSnapshot EMPTY = new StateSnapshot<Integer>(null);
}
