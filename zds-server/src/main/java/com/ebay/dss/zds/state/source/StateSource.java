package com.ebay.dss.zds.state.source;

import com.ebay.dss.zds.state.StateSnapshot;

import java.io.Serializable;
import java.util.Properties;

/**
 * Created by tatian on 2020-08-25.
 */
public abstract class StateSource {

  protected Properties prop;

  public StateSource(Properties prop) {
    this.prop = prop;
    reload();
  }

  public abstract boolean store(String key, StateSnapshot stateSnapshot);
  public abstract <T extends Serializable> StateSnapshot<T> lookup(String key);
  public abstract void remove(String key);
  public abstract void reload();

}
