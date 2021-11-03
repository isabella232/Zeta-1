package com.ebay.dss.zds.rest.error.match;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoadExceptionRuleTask implements Runnable {
  private static Logger logger = LoggerFactory.getLogger(LoadExceptionRuleTask.class);
  private ExceptionRuleService service;

  public LoadExceptionRuleTask(ExceptionRuleService service) {
    this.service = service;
  }

  @Override
  public void run() {
    try {
      service.refreshRuleQueries();
    } catch( Throwable t) {
      logger.error("Error when loading exception rule: ", t);
    }
  }
}
