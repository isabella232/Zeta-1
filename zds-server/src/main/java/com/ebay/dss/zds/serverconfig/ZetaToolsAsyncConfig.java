package com.ebay.dss.zds.serverconfig;


import com.ebay.dss.zds.common.PropertiesUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Created by zhouhuang on 2019年1月20日
 */
@Configuration
@EnableAsync
public class ZetaToolsAsyncConfig {

  @Bean(name = "dataMoveTaskExecutor")
  public Executor dataMoveTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    if (PropertiesUtil.isScheduleSwitchOn(PropertiesUtil.ScheduleSwitchKey.DataMove)) {
      executor.setCorePoolSize(50);
      executor.setMaxPoolSize(200);
      executor.setWaitForTasksToCompleteOnShutdown(true);
      executor.setThreadNamePrefix("DataMove-");
      executor.initialize();
    }
    return executor;

  }

  @Bean(name = "schedulerTaskExecutor")
  public Executor schedulerTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    if (PropertiesUtil.isScheduleSwitchOn(PropertiesUtil.ScheduleSwitchKey.SCHEDULER)) {
      executor.setCorePoolSize(100);
      executor.setMaxPoolSize(500);
      executor.setWaitForTasksToCompleteOnShutdown(true);
      executor.setThreadNamePrefix("Scheduler-");
      executor.initialize();
    }
    return executor;
  }

  @Bean(name = "metaTableTaskExecutor")
  public Executor metaTableTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(50);
    executor.setMaxPoolSize(200);
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setThreadNamePrefix("MetaTable-");
    executor.initialize();
    return executor;
  }

  @Bean(name = "sundryTaskExecutor")
  public Executor sundryTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(3);
    executor.setMaxPoolSize(5);
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setQueueCapacity(10);
    executor.setThreadNamePrefix("Sundry-");
    executor.initialize();
    return executor;
  }

  @Bean(name = "asyncEventExecutor")
  public Executor asyncEventExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(50);
    executor.setMaxPoolSize(200);
    executor.setThreadNamePrefix("AsyncEvent-");
    executor.initialize();
    return executor;
  }
}
