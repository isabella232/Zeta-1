package com.ebay.dss.zds.cluster;

import com.ebay.dss.zds.common.QueueDestination;

/**
 * Created by tatian on 2021-02-05.
 */
public interface QueueDestinationMaker {
  QueueDestination makeDestination(String user);
}
