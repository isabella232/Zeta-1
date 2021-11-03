package com.ebay.dss.zds.service.schedule;

import com.ebay.dss.zds.common.JsonUtil;
import com.ebay.dss.zds.model.DataValidateDetail;
import com.ebay.dss.zds.model.schedule.ScheduleHistory;
import com.ebay.dss.zds.model.schedule.ScheduleJob;
import com.ebay.dss.zds.service.DataValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class DataValidateScheduler extends Scheduler {

  private static final Logger LOGGER = LoggerFactory.getLogger(DataValidateScheduler.class);

  @Autowired
  DataValidationService dataValidationService;

  void startJob(ScheduleHistory scheduleHistory) {
    ScheduleJob scheduleJob = scheduleHistory.getScheduleJob();
    LOGGER.info("Thread [{}],Start to handle Data Validate {}",
        Thread.currentThread().getName(), scheduleJob);
    scheduleHistory.setRunTime(new Date());
    DataValidateDetail dataValidateDetail = JsonUtil.fromJson(
        scheduleJob.getTask(), DataValidateDetail.class);
    dataValidationService.save(dataValidateDetail);
    scheduleHistory.setJobHistoryId(dataValidateDetail.getHistory().getHistoryId());
    scheduleHistoryRepository.save(scheduleHistory);
  }

  void cancelJob(ScheduleHistory scheduleHistory) {
    //todo
  }

  String getJobFailLog(ScheduleHistory scheduleHistory) {
    //todo
    return null;
  }
}
