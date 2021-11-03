package com.ebay.dss.zds.interpreter.interpreters.livy;

import java.util.List;

/**
 * Created by tatian on 2020-02-18.
 */
public interface LivyURLSelector {

  public String select();
  public boolean hasNext();
  public String removeUrl(String url);
  public List<String> urls();
}
