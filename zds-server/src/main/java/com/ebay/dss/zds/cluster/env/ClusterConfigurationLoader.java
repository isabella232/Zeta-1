package com.ebay.dss.zds.cluster.env;

import java.util.Properties;

/**
 * Created by tatian on 2020-11-21.
 */
public interface ClusterConfigurationLoader<C extends ClusterEnv> {

  void doLoad(C c, Properties properties);

}
