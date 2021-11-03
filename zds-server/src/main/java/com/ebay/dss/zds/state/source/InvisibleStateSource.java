package com.ebay.dss.zds.state.source;

import com.ebay.dss.zds.state.StateSnapshot;

import java.util.Properties;

/**
 * Created by tatian on 2020-09-18.
 */
public class InvisibleStateSource extends StateSource {

  public InvisibleStateSource(Properties prop) {
    super(prop);
  }

  @Override
  public void reload() {}

  public boolean store(String key, StateSnapshot stateSnapshot) {
    return false;
  }

  public StateSnapshot lookup(String key) {
    return null;
  }
  public void remove(String key) {}
}
