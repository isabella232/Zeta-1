package com.ebay.dss.zds.service.schedule;

import com.ebay.dss.zds.common.SchedulerQueueDestination;
import com.ebay.dss.zds.dao.ScheduleHistoryRepository;
import com.ebay.dss.zds.exception.InterpreterExecutionException;
import com.ebay.dss.zds.model.schedule.ScheduleHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import com.ebay.dss.zds.model.schedule.ScheduleHistory.JobRunStatus;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import static com.ebay.dss.zds.model.schedule.ScheduleHistory.JobRunStatus.*;
import static com.ebay.dss.zds.service.schedule.ScheduleConstant.*;
import static com.ebay.dss.zds.service.schedule.ScheduleJobType.DataMove;
import static com.ebay.dss.zds.service.schedule.ScheduleJobType.DataValidate;
import static com.ebay.dss.zds.service.schedule.hook.ScheduleHookSubject.ScheduleHook;

@Component
public abstract class Scheduler {
  private final static Logger LOGGER = LoggerFactory.getLogger(Scheduler.class);

  @Autowired
  ScheduleHistoryRepository scheduleHistoryRepository;

  @Autowired
  private ScheduleEmailService scheduleEmailService;

  @Autowired
  ScheduleOperationTrack operationTrack;


  @Resource(name = "schedulerTaskExecutor")
  private ThreadPoolTaskExecutor executor;

  public static CopyOnWriteArraySet<String> RUNNING_JOBS = new CopyOnWriteArraySet<>();
  public static CopyOnWriteArraySet<Long> FINISH_JOBS = new CopyOnWriteArraySet<>();

  abstract void startJob(ScheduleHistory scheduleHistory);

  abstract void cancelJob(ScheduleHistory scheduleHistory);

  abstract String getJobFailLog(ScheduleHistory scheduleHistory);

  ScheduleHistory initJob(ScheduleHistory history) {
    try {
      if (!isJobRunning(history)) {
        switch (history.getJobRunStatus()) {
          case WAITING:
            return history;
          case PENDING:
            history.setJobRunStatus(WAITING);
            scheduleHistoryRepository.updateJobRunStatus(
                WAITING.name(), history.getId());
            return history;
        }
      }
    } catch (Exception e) {
      LOGGER.error("Job init failed!", e);
    }
    return null;
  }

  public void finishJob(ScheduleHistory history) {
    LOGGER.info("Job is finished - {}", history);
    ScheduleHistory updateHistory = scheduleHistoryRepository
        .findById(history.getId()).get();
    if (updateHistory.getJobRunStatus() == CANCELED) {
      LOGGER.info("Job have been canceled - {}", updateHistory);
      history.setJobRunStatus(updateHistory.getJobRunStatus());
      history.setLog(updateHistory.getLog());
      history.setJobHistoryId(updateHistory.getJobHistoryId());
    }
    history.setEndTime(new Date());
    scheduleHistoryRepository.save(history);
    try {
      scheduleEmailService.sendScheduleMail(history,
          history.getJobRunStatus() == FAIL ? getJobFailLog(history) : null);
      closeJobSessionIfNeeded(history);
    } catch (Exception e) {
      LOGGER.info("Finish Ops failed!", e);
    }
    operationTrack.trackOperationLog(history.getId().toString(),
        SYSTEM_ACCOUNT, history.getJobRunStatus().name());
  }

  private void closeJobSessionIfNeeded(ScheduleHistory history) {
    cancelJob(history);
  }

  void handleJobInstanceList(List<ScheduleHistory> scheduleHistoryList) {
    Optional<ScheduleHistory> runningInstance = scheduleHistoryList.stream()
        .filter(i -> i.getJobRunStatus() == RUNNING)
        .findFirst();
    LOGGER.info("Running instance is: {}", runningInstance);
    scheduleHistoryList.forEach(i -> handleJobInstance(i, runningInstance.isPresent()));
  }

  public void handleJobInstance(ScheduleHistory history, boolean isExistJobRunning) {
    LOGGER.info("Handle Schedule Job instance [{}], status is {}",
        history.getId(), history.getJobRunStatus());
    switch (history.getJobRunStatus()) {
      case PENDING:
      case WAITING:
        if (!isExistJobRunning) {
          Optional.ofNullable(initJob(history)).ifPresent(this::setup);
        }
        break;
      case RUNNING:
        Optional.ofNullable(checkJobRunningStatus(history)).ifPresent(this::setup);
        break;
      case CANCELED:
        Optional.ofNullable(checkJobCanceledStatus(history)).ifPresent(this::cancelJob);
        break;
    }
  }

  private ScheduleHistory checkJobRunningStatus(ScheduleHistory history) {
    if (!isJobRunning(history)) {
      if (Objects.nonNull(history.getJobOperation())) {
        markJobOperationDone(history);
        return history;
      } else if (Objects.nonNull(history.getJobHistoryId())) {
        ScheduleJobType type = ScheduleJobType.valueOf(history.getScheduleJob().getType());
        return type == DataMove || type == DataValidate ? null : history;
      } else if (!isJobInstanceFinished(history)) {
        return history;
      }
    }
    return null;
  }

  private ScheduleHistory checkJobCanceledStatus(ScheduleHistory history) {
    if (Objects.nonNull(history.getJobOperation())) {
      markJobOperationDone(history);
      if (isJobInstanceRunning(history)) {
        return history;
      }
    }
    return null;
  }

  private void markJobOperationDone(ScheduleHistory history) {
    history.setJobOperation(null);
    scheduleHistoryRepository.save(history);
  }

  private void handleException(ScheduleHistory history, Exception e) {
    history.setJobRunStatus(FAIL);
    history.setLog(optimizeExceptionLog(e));
    finishJob(history);
  }

  public void setup(ScheduleHistory history) {
    LOGGER.info("Job start to run - {}", history);
    RUNNING_JOBS.add(getScheduleJobToken(history));
    try {
      executor.submit(() -> {
        try {
          history.setJobRunStatus(RUNNING);
          scheduleEmailService.sendScheduleMail(history);
          startJob(history);
        } catch (Exception e) {
          LOGGER.error("{} Execute Failed", history, e);
          handleException(history, e);
        } finally {
          teardown(history);
        }
      });
    } catch (Exception e) {
      LOGGER.error("Submit task failed!", e);
      teardown(history);
    }
    operationTrack.trackOperationLog(history.getId().toString(),
        SYSTEM_ACCOUNT, SYS_START);
  }


  private String getScheduleJobToken(ScheduleHistory history) {
    return String.format("%s_%s", history.getScheduleJob().getId(), history.getId());
  }

  public void teardown(ScheduleHistory history) {
    LOGGER.info("Job Start to teardown - {}", history);
    FINISH_JOBS.add(history.getId());
    ScheduleHook.tearDown(history);
    RUNNING_JOBS.remove(getScheduleJobToken(history));
    if (history.getJobRunStatus() == SUCCESS) {
      startNextInstance(history);
    }
  }

  private void startNextInstance(ScheduleHistory history) {
    Optional<ScheduleHistory> nextInstance = scheduleHistoryRepository
        .findFirstByScheduleJobAndIdGreaterThanOrderById(history.getScheduleJob(), history.getId());
    LOGGER.info("Next Job Instance is: {}", nextInstance);
    nextInstance.ifPresent(i -> handleJobInstance(i, false));
  }


  private boolean isJobRunning(ScheduleHistory history) {
    Optional<String> job = RUNNING_JOBS.stream().filter(
        i -> i.startsWith(history.getScheduleJob().getId() + "_")).findFirst();
    if (job.isPresent()) {
      LOGGER.info("Running job list - {}", RUNNING_JOBS);
      LOGGER.info("Job [{}] is running", job);
      return true;
    }
    return false;
  }

  private boolean isJobInstanceRunning(ScheduleHistory history) {
    if (RUNNING_JOBS.contains(getScheduleJobToken(history))) {
      LOGGER.info("Running job list - {}", RUNNING_JOBS);
      LOGGER.info("Job instance [{}] is running", getScheduleJobToken(history));
      return true;
    }
    return false;
  }

  private boolean isJobInstanceFinished(ScheduleHistory history) {
    if (FINISH_JOBS.contains(history.getId())) {
      LOGGER.info("FINISHED job list - {}", FINISH_JOBS);
      LOGGER.info("Job instance [{}] is finished", history.getId());
      return true;
    }
    return false;
  }

  @Deprecated
  private boolean isLastJobInstanceSuccess(ScheduleHistory history) {
    Optional<ScheduleHistory> scheduleHistory =
        scheduleHistoryRepository.findFirstByScheduleJobAndIdLessThanOrderByIdDesc(
            history.getScheduleJob(), history.getId());
    return !scheduleHistory.isPresent() ||
        scheduleHistory.get().getJobRunStatus().equals(JobRunStatus.SUCCESS);
  }

  // for data move & data validate
  public ScheduleHistory updateScheduleHistoryStatus(Long jobHistoryId, boolean isSucceed
      , String log, ScheduleJobType type) {
    LOGGER.info("Get {} Schedule Job History By Id {}", type, jobHistoryId);
    ScheduleHistory scheduleHistory = scheduleHistoryRepository
        .findByJobHistoryIdAndType(jobHistoryId, type.name());
    LOGGER.info("Update {} Schedule Job History: {}", type, scheduleHistory);
    if (Objects.nonNull(scheduleHistory)) {
      scheduleHistory.setJobRunStatus(isSucceed ? SUCCESS : FAIL);
      scheduleHistory.setLog(log);
      LOGGER.info("Update {} Schedule Job History: {}", type, scheduleHistory);
      finishJob(scheduleHistory);
    }
    return scheduleHistory;
  }

  public String optimizeExceptionLog(Exception e) {
    LOGGER.error("Optimize Exception Log", e);
    try {
      if (e instanceof InterpreterExecutionException) {
        return ((InterpreterExecutionException) e)
            .getError().getErrors().stream()
            .map(SchedulerQueueDestination::optimizeLog)
            .collect(Collectors.joining(","));
      } else {
        return Optional.ofNullable(e.getMessage())
            .orElse("Failed with Unknown error!");
      }
    } catch (Exception ex) {
      return e.getMessage();
    }
  }
}