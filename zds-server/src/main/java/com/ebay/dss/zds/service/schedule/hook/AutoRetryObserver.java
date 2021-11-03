package com.ebay.dss.zds.service.schedule.hook;

import com.ebay.dss.zds.common.JsonUtil;
import com.ebay.dss.zds.common.PropertiesUtil;
import com.ebay.dss.zds.dao.ScheduleHistoryRepository;
import com.ebay.dss.zds.exception.ScheduleException;
import com.ebay.dss.zds.model.schedule.ScheduleHistory;
import com.ebay.dss.zds.service.schedule.*;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static com.ebay.dss.zds.model.schedule.ScheduleHistory.JobRunStatus.*;
import static com.ebay.dss.zds.service.schedule.ScheduleConstant.*;
import static com.ebay.dss.zds.service.schedule.ScheduleJobType.Notebook;
import static com.ebay.dss.zds.service.schedule.Scheduler.FINISH_JOBS;
import static com.ebay.dss.zds.service.schedule.hook.ScheduleHookSubject.ScheduleHook;

@Component
public class AutoRetryObserver extends ScheduleHistoryObserver {

  private static final Logger LOGGER = LoggerFactory.getLogger(AutoRetryObserver.class);

  @Autowired
  ScheduleOperationTrack operationTrack;

  @Autowired
  ScheduleHistoryRepository scheduleHistoryRepository;

  @Autowired
  ScheduleEmailService emailService;

  @Autowired
  NotebookUtilService notebookUtilService;

  static ConcurrentHashMap<Long, Integer> RETRY_MAP = new ConcurrentHashMap<>();

  public AutoRetryObserver() {
    this.subject = ScheduleHook;
    this.subject.addObserver(this, 0);
  }

  @Override
  public void update(ScheduleHistory history) {
    LOGGER.info("{} receive update signal", this.getClass().getName());
    try {
      if (isJobRetryEnabled(history)) {
        if (scheduleHistoryRepository.updateJobRunStatusIfNotCancelled(
            AUTORETRY.name(), history.getId()) > 0) {
          history.setJobRunStatus(AUTORETRY);
          trackAutoRetryLog(history, SYS_RETRY);
          recoverJobStatus(history, SCHEDULE_AUTO_RETRY_FAIL_HERMES_DOWN);
        }
      }
    } catch (InterruptedException e) {
      LOGGER.error("Thread sleep is interrupted!", e);
    }


  }

  private boolean isJobRetryEnabled(ScheduleHistory history) throws InterruptedException {
    LOGGER.info("Check whether schedule job instance can be retry: {}", JsonUtil.toJson(history));
    try {
      return history.getScheduleJob().isAutoRetry()
          && history.getJobRunStatus() == FAIL
          && isJobSessionConnected(history)
          && checkAutoRetryError(history)
          && checkJobRetryTimes(history);
    } catch (ScheduleException e) {
      recoverJobStatus(history, e.getMessage());
    }
    return false;
  }

  private boolean isJobSessionConnected(ScheduleHistory history) {
    LOGGER.info("Check whether schedule job instance [{}] is connected", history.getId());
    if (history.getJobHistoryId() == null) {
      throw new ScheduleException(SCHEDULE_AUTO_RETRY_FAIL_HERMES_DOWN);
    }
    return true;
  }

  private boolean checkAutoRetryError(ScheduleHistory history) {
    String error = PropertiesUtil.getScheduleProperties("auto_retry_error");
    LOGGER.info("Get Schedule auto retry error: {}", error);
    try {
      for (String err : JsonUtil.fromJson(error, new TypeReference<List<String>>() {
      })) {
        if (history.getLog().toLowerCase().contains(err)) {
          return true;
        }
      }
    } catch (Exception e) {
      LOGGER.info("Check auto retry log failed!", e);
    }
    throw new ScheduleException(SCHEDULE_AUTO_RETRY_FAIL_OTHER_ERROR);
  }

  private boolean checkJobRetryTimes(ScheduleHistory history) {
    LOGGER.info("Check whether schedule job instance [{}] retry number", history.getId());
    return RETRY_MAP.getOrDefault(history.getId(), 0) < 3;
  }

  private void trackAutoRetryLog(ScheduleHistory history, String desc, String... comments) {
    switch (desc) {
      case SYS_RETRY:
        operationTrack.trackOperationLog(history.getId().toString(), SYSTEM_ACCOUNT, SYS_RETRY);
        emailService.sendScheduleMail(history);
        break;
      case SYS_RETRY_SUCCESS:
        operationTrack.trackOperationLog(history.getId().toString(), SYSTEM_ACCOUNT, SYS_RETRY_SUCCESS);
        emailService.sendScheduleMail(history, null, RETRYSUCCESS.name());
        break;
      case SYS_RETRY_FAIL:
        operationTrack.trackOperationLog(history.getId().toString(), SYSTEM_ACCOUNT, SYS_RETRY_FAIL, comments);
        String failLog = comments.length > 0 ? String.format("<p>Fail Reason: %s</p>", comments[0]) : null;
        emailService.sendScheduleMail(history, failLog, RETRYFAIL.name());
        break;

    }
  }

  private void addJobRetryTimes(ScheduleHistory history) {
    RETRY_MAP.put(history.getId(),
        RETRY_MAP.getOrDefault(history.getId(), 0) + 1);
  }

  private void autoRetryJob(ScheduleHistory history) {
    ScheduleHistory latestHistory = scheduleHistoryRepository
        .findById(history.getId()).get();
    if (latestHistory.getJobRunStatus() == CANCELED) {
      LOGGER.info("Schedule job instance {} is canceled, stop retry", history.getId());
      return;
    }
    history.setJobRunStatus(RUNNING);
    history.setRunTime(new Date());
    history.setEndTime(null);
    history.setLog(null);
    history.setJobHistoryId(null);
    LOGGER.info("Reset schedule job instance: {}", JsonUtil.toJson(history));
    scheduleHistoryRepository.save(history);
    FINISH_JOBS.remove(history.getId());
    trackAutoRetryLog(history, SYS_RETRY_SUCCESS);
  }

  private void recoverJobStatus(ScheduleHistory history, String retryFailMsg) {
    if (scheduleHistoryRepository.updateJobRunStatusIfNotCancelled(
        FAIL.name(), history.getId()) > 0) {
      trackAutoRetryLog(history, SYS_RETRY_FAIL, retryFailMsg);
    }
  }

  private void waitingToCheckHermesQueueStatus(ScheduleHistory history) throws InterruptedException {
    scheduleHistoryRepository.updateJobRunStatus(
        AUTORETRYWAITING.name(), history.getId());
    Thread.sleep(60000);
  }
}
