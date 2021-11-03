package com.ebay.dss.zds.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.RejectedExecutionException;

import static com.ebay.dss.zds.common.PropertiesUtil.getBoolean;

/**
 * Created by tatian on 2020-08-29.
 */
public class ExecutionScheduler {

  protected static final Logger logger = LoggerFactory.getLogger(ExecutionScheduler.class);

  public static final String CREATE_ON_ABSENT = "zds.execution.pool.executor.createOnAbsent";
  public static final String SHARD_TENANT = "shared";
  public static final String IMMEDIATE_TENANT = "immediate";

  private Properties properties;
  private boolean createOnAbsent;
  private ExecutionDispatcher dispatcher;
  private Map<String, Tenant> planedTenants;

  private static ExecutionScheduler _instance;

  private ExecutionScheduler(Properties prop) {
    this.properties = prop;
    this.dispatcher = ExecutionDispatcher.singleton(properties);
    this.planedTenants = createTenants(prop);
    this.createOnAbsent = getBoolean(CREATE_ON_ABSENT, prop, false);
  }

  public static synchronized ExecutionScheduler singleton(Properties prop) {
    if (_instance == null) {
      _instance = new ExecutionScheduler(prop);
    }
    return _instance;
  }

  protected ExecutionDispatcher getDispatcher() {
    return this.dispatcher;
  }

  private static Tenant immediate() {
    Tenant.TenantBuilder builder = new Tenant.TenantBuilder(IMMEDIATE_TENANT);
    builder.corePoolSize(0);
    builder.maxPoolSize(1024);
    builder.queueCapacity(0);
    builder.keepAliveSeconds(0);
    builder.taskStartTimeout(-1);
    return builder.build();
  }

  private static Tenant shared() {
    Tenant.TenantBuilder builder = new Tenant.TenantBuilder(SHARD_TENANT);
    builder.fallbacks(IMMEDIATE_TENANT);
    return builder.build();
  }

  private Map<String, Tenant> createTenants(Properties properties) {

    Map<String, Tenant> tenantMap = Tenant.fromProperties(properties);

    // always create a immediate tenant here
    if (!tenantMap.containsKey(IMMEDIATE_TENANT)) {
      logger.info("There is no immediate tenant in properties file, need to create one");
      tenantMap.put(IMMEDIATE_TENANT, immediate());
    } else {
      Tenant tenant = tenantMap.get(IMMEDIATE_TENANT);
      tenant.setTaskStartTimeout(-1);
    }

    // always create a shared tenant here
    if (!tenantMap.containsKey(SHARD_TENANT)) {
      logger.info("There is no shared tenant in properties file, need to create one");
      tenantMap.put(SHARD_TENANT, shared());
    } else {
      Tenant tenant = tenantMap.get(SHARD_TENANT);
      tenant.addFallbackTenants(IMMEDIATE_TENANT);
    }

    tenantMap.keySet().forEach(tenantName -> dispatcher.initExecutor(tenantName, tenantMap.get(tenantName).getProp()));

    return tenantMap;
  }

  public final String schedule(String tenantName, Runnable runnable) {
    logger.info("Choosing tenant to run");
    // ideally we hope will always find the tenant here to avoid create it when execution
    String finalTenant;
    if (dispatcher.hasTenant(tenantName) || createOnAbsent) {
      finalTenant = tenantName;
    } else {
      // if no tenant found and createOnAbsent is false, we shall put it in the shared tenant
      // this shared tenant should be created when initializing
      finalTenant = SHARD_TENANT;
    }
    try {
      dispatcher.dispatch(finalTenant, runnable);
      logger.info("Tenant: {} dispatched the task", finalTenant);
      return finalTenant;
    } catch (RejectedExecutionException jee) {
      logger.error("Tenant: {} reject the task, try to use fallback tenant", finalTenant);
      Tenant tenant = planedTenants.get(finalTenant);
      if (tenant == null || tenant.fallbackTenants().size() == 0) {
        logger.error("No any fallback tenant found of {}, reject task", finalTenant);
        throw jee;
      } else {
        /** only allow one level fallbacks, so do not use recurse here **/
        List<String> fallbacks = tenant.fallbackTenants();
        boolean findTenant = false;
        for (String fallbackTenant : fallbacks) {
          try {
            if (dispatcher.hasTenant(fallbackTenant)) {
              dispatcher.dispatch(fallbackTenant, runnable);
              finalTenant = fallbackTenant;
              findTenant = true;
              logger.info("Used fallback tenant: {} to execute the task instead", fallbackTenant);
              break;
            } else {
              logger.error("Un-planed fallback tenant: {} can't be used", fallbackTenant);
            }
          } catch (RejectedExecutionException again) {
            logger.error("Fallback tenant: {} reject the task too...", fallbackTenant);
          }
        }
        if (!findTenant) {
          logger.error("Still can not found fallback tenant of {}, reject task", finalTenant);
          throw jee;
        } else {
          return finalTenant;
        }
      }
    }
  }

  @NotNull
  public Map<String, String> showTenants() {
    return this.dispatcher.showTenants();
  }

  public void shutdown(boolean force) {
    this.dispatcher.destroy(force);
  }

}
