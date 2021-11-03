package com.ebay.dss.zds.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by tatian on 2020-08-27.
 */
@Component
public class ZetaExecutionPool {

  protected static final Logger logger = LoggerFactory.getLogger(ZetaExecutionPool.class);

  @Autowired
  @Qualifier("EnvironmentProperties")
  private Properties properties;

  @Value("${zds.execution.pool.enable:#{true}}")
  private volatile boolean enabled;

  private ExecutionScheduler scheduler;

  @PostConstruct
  private void initDispatcher() {
    if (enabled) {
      logger.info("***********************The ZetaExecutionPool is enabled***********************");
      this.scheduler = ExecutionScheduler.singleton(properties);
    }
  }

  public final void submit(String tenantName, Runnable runnable) {
    if (enabled) {
      scheduler.schedule(tenantName, runnable);
    } else {
      // run with current thread
      logger.info("The ZetaExecutionPool is not enabled, run with current thread");
      runnable.run();
    }
  }

  public boolean isEnabled() {
    return enabled;
  }

  @NotNull
  public Map<String, String> showTenants() {
    if (isEnabled()) {
      return this.scheduler.showTenants();
    } else {
      logger.info("The Zeta execution pool is not enabled");
      return new HashMap<>();
    }
  }

  public void enable() {
    this.enabled = true;
  }

  public void disable() {
    this.enabled = false;
  }

  public synchronized void initIfNoDispatcher() {
    if (enabled && this.scheduler == null) {
      initDispatcher();
    }
  }

  public synchronized void setProperties(Properties properties) {
    this.properties = properties;
  }

}
