package com.ebay.dss.zds.service.schedule;

import com.ebay.dss.zds.common.PropertiesUtil;
import com.ebay.dss.zds.service.DataValidationClient;
import com.ebay.dss.zds.service.datamove.DataMoverClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.ebay.dss.zds.common.PropertiesUtil.ScheduleSwitchKey;

@Component
public class ScheduleTaskClient {

  @Autowired
  SchedulerClient schedulerClient;

  @Autowired
  DataValidationClient dataValidationClient;

  @Autowired
  DataMoverClient dataMoverClient;

  @Scheduled(fixedDelay = 60 * 1000)
  public void generateScheduleJob() {
    if (PropertiesUtil.isScheduleSwitchOn(ScheduleSwitchKey.SCHEDULER)) {
      schedulerClient.generateScheduleJobInstance();
    }
  }

  @Scheduled(fixedDelay = 60 * 1000)
  public void executeScheduleJob() {
    if (PropertiesUtil.isScheduleSwitchOn(ScheduleSwitchKey.SCHEDULER)) {
      schedulerClient.executeScheduleJob();
    }
  }

  @Scheduled(cron = "0 1 0 * * ?")
  public void cleanInactivateScheduleJob() {
    if (PropertiesUtil.isScheduleSwitchOn(ScheduleSwitchKey.SCHEDULER)) {
      schedulerClient.cleanInactivateScheduleJob();
    }
  }

  @Scheduled(cron = "0 0 1 ? * MON")
  public void cleanScheduleJob() {
    if (PropertiesUtil.isScheduleSwitchOn(ScheduleSwitchKey.SCHEDULER)) {
      schedulerClient.cleanScheduleJob();
    }
  }

  @Scheduled(fixedDelay = 60 * 1000)
  public void executeDatavaliate() {
    if (PropertiesUtil.isScheduleSwitchOn(ScheduleSwitchKey.DataValidate)) {
      dataValidationClient.executeDatavaliate();
    }
  }

  @Scheduled(fixedDelay = 60 * 1000)
  public void executeDataMove() {
    if (PropertiesUtil.isScheduleSwitchOn(ScheduleSwitchKey.DataMove)) {
      dataMoverClient.executeDataMove();
    }
  }

  @Scheduled(fixedRate = 60 * 1000)
  public void executeDataMoveCronTabTask() {
    if (PropertiesUtil.isScheduleSwitchOn(ScheduleSwitchKey.DataMoveCron)) {
      dataMoverClient.executeDataMoveCronTabTask();
    }
  }

  @Scheduled(cron = "0 0 1 * * ?")
  public void cleanLocalFile() {
    if (PropertiesUtil.isScheduleSwitchOn(ScheduleSwitchKey.DataMoveCleanFile)) {
      dataMoverClient.cleanLocalFile();
    }
  }

  @Scheduled(fixedDelay = 10 * 60 * 1000)
  public void reLoadProperties() {
    PropertiesUtil.loadSchedulerConfiguration();
  }
}
