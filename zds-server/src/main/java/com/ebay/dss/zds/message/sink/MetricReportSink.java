package com.ebay.dss.zds.message.sink;

import com.ebay.dss.zds.message.ZetaEvent;
import com.ebay.dss.zds.message.ZetaEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by tatian on 2019-11-28.
 */
public class MetricReportSink implements ZetaEventListener {

  private static final Logger logger = LoggerFactory.getLogger(MetricReportSink.class);

  @Override
  public void onEventReceived(ZetaEvent zetaEvent) {
    try {
    } catch (Exception ex) {
      logger.error(ex.toString());
    }
  }

}
