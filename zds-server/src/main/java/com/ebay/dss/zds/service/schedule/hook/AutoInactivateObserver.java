package com.ebay.dss.zds.service.schedule.hook;

import com.ebay.dss.zds.dao.ScheduleHistoryRepository;
import com.ebay.dss.zds.dao.ScheduleJobRepository;
import com.ebay.dss.zds.model.schedule.ScheduleHistory;
import com.ebay.dss.zds.model.schedule.ScheduleJob;
import com.ebay.dss.zds.service.schedule.ScheduleEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static com.ebay.dss.zds.model.schedule.ScheduleHistory.JobRunStatus.FAIL;
import static com.ebay.dss.zds.service.schedule.hook.ScheduleHookSubject.ScheduleHook;

@Component
public class AutoInactivateObserver extends ScheduleHistoryObserver {

  private static Logger LOGGER = LoggerFactory.getLogger(AutoInactivateObserver.class);

  @Autowired
  ScheduleHistoryRepository scheduleHistoryRepository;

  @Autowired
  ScheduleJobRepository scheduleJobRepository;

  @Autowired
  ScheduleEmailService mailService;

  public AutoInactivateObserver() {
    this.subject = ScheduleHook;
    this.subject.addObserver(this);
  }

  public void update(ScheduleHistory history) {
    LOGGER.info("{} receive update signal", this.getClass().getName());
    if (history.getJobRunStatus() == FAIL) {
      int failTimes = history.getScheduleJob().getFailTimesToBlock();
      LOGGER.info("Schedule job {} fail times to block is {}",
          history.getScheduleJob().getId(), failTimes);
      if (failTimes > 0) {
        checkFailTimesToBlock(history, failTimes);
      }
    }
  }

  private void checkFailTimesToBlock(ScheduleHistory history, int failTimes) {
    List<ScheduleHistory> histories = scheduleHistoryRepository
        .findLatestJobHistoriesByLimitTo(history.getScheduleJob().getId(), history.getId(), failTimes);
    LOGGER.info("Check job {} latest history list size is {}", history.getId(), histories.size());
    List<ScheduleHistory> failedHistories = histories.stream()
        .filter(h -> h.getJobRunStatus() == FAIL).collect(Collectors.toList());
    LOGGER.info("Check job {} latest fail history list size is {}", history.getId(), failedHistories.size());
    if (failedHistories.size() == failTimes) {
      autoSuspendJob(history.getScheduleJob());
    }
  }

  private void autoSuspendJob(ScheduleJob scheduleJob) {
    Long jobId = scheduleJob.getId();
    LOGGER.info("Auto suspend job {}", jobId);
    if (scheduleJobRepository.updateScheduleJobStatus(0, jobId) > 0) {
      mailService.sendScheduleSuspendEmail(jobId, scheduleJob.getJobName(),
          scheduleJob.getFailTimesToBlock(), scheduleJob.getNt() + "@ebay.com");
    }
  }
}
