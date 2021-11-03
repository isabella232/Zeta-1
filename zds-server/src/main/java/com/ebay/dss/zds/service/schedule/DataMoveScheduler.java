package com.ebay.dss.zds.service.schedule;

import com.ebay.dss.zds.common.JsonUtil;
import com.ebay.dss.zds.model.DataMoveDetail;
import com.ebay.dss.zds.model.schedule.ScheduleHistory;
import com.ebay.dss.zds.model.schedule.ScheduleJob;
import com.ebay.dss.zds.service.datamove.DataMoveJobType;
import com.ebay.dss.zds.service.datamove.DataMoveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

import static com.ebay.dss.zds.service.datamove.DataMoveJobType.*;

@Component
public class DataMoveScheduler extends Scheduler {

  private static final Logger LOGGER = LoggerFactory.getLogger(DataMoveScheduler.class);

  @Autowired
  DataMoveService dataMoveService;

  void startJob(ScheduleHistory scheduleHistory) {
    ScheduleJob scheduleJob = scheduleHistory.getScheduleJob();
    LOGGER.info("Thread [{}],Start to handle Data Move {}",
        Thread.currentThread().getName(), scheduleJob);
    scheduleHistory.setRunTime(new Date());
    DataMoveDetail dataMoveDetail = JsonUtil.fromJson(
        scheduleJob.getTask(), DataMoveDetail.class);
    DataMoveJobType dataMoveJobType = dataMoveDetail.getHistory().getType() ==
        VDM2HD.getId() ? VDM2HD : TD2HD;
    dataMoveService.save(dataMoveDetail, dataMoveJobType);
    scheduleHistory.setJobHistoryId(dataMoveDetail.getHistory().getHistoryId());
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
