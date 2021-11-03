package com.ebay.dss.zds.service.datamove;

import com.ebay.dss.zds.dao.DataMoveRepository;
import com.ebay.dss.zds.exception.ClusterAccessDeniedException;
import com.ebay.dss.zds.interpreter.InterpreterManager;
import com.ebay.dss.zds.model.*;
import com.ebay.dss.zds.service.MailService;
import com.ebay.dss.zds.service.schedule.DataMoveScheduler;
import com.google.common.collect.ImmutableMap;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.ebay.dss.zds.service.datamove.DataMoveJobType.*;
import static com.ebay.dss.zds.service.schedule.ScheduleJobType.DataMove;

@Component
public abstract class DataMoveJob {

  private static final Logger LOGGER = LoggerFactory.getLogger(DataMoveJob.class);

  @Autowired
  protected DataMoveRepository dataMoveRepository;

  @Autowired
  public HadoopTableOperation hadoopTableOperation;

  @Autowired
  private DataMoveScheduler dataMoveScheduler;

  @Autowired
  private MailService mailService;

  // Queue
  public static ConcurrentHashMap<Long, Integer> RETRY_NUMS = new ConcurrentHashMap<>();
  public static CopyOnWriteArrayList<String> RUNNING_USERS = new CopyOnWriteArrayList<>();
  public static CopyOnWriteArrayList<Long> FINISHED_JOBS = new CopyOnWriteArrayList<>();


  // log
  private static final Map<String, String> logMap = ImmutableMap.of(
      "Account is invalid",
      "TDPWDERROR",
      "access to syslib.contract",
      "TDBACCGERROR",
      "java.sql.SQLException: [Teradata Database]",
      "TDBGERROR",
      "make sure the queue has enough resources",
      "RESOURCEERROR");


  abstract void initialize(DataMoveDetail dataMoveDetail);

  abstract void startMove(DataMoveDetail dataMoveDetail);

  public void checkResult(DataMoveDetail dataMoveDetail) {
    try {
      if (checkJobFinished(dataMoveDetail) && (!checkJobNeedRetry(dataMoveDetail))) {
        LOGGER.info("Data Move Job Done - {}.", dataMoveDetail);
        FINISHED_JOBS.addIfAbsent(dataMoveDetail.getHistory().getHistoryId());
        dataMoveDetail.getHistory().setEndTime(new Date());
        // update schedule job status if exist
        boolean isScheduleJob = Optional.ofNullable(dataMoveScheduler.updateScheduleHistoryStatus(
            dataMoveDetail.getHistory().getHistoryId(),
            dataMoveDetail.getHistory().getStatus() == 1,
            dataMoveDetail.getHistory().getLog(), DataMove)).isPresent();
        if (!isScheduleJob && (DataMoveJobType.idOf(dataMoveDetail.getHistory().getType()) == TD2HD
            || DataMoveJobType.idOf(dataMoveDetail.getHistory().getType()) == LC2HD)) {
          mailService.sendDataMoveDoneEmail(dataMoveDetail);
        }
      }
    } catch (Exception e) {
      LOGGER.error("Check result failed", e);
    }
    LOGGER.info("Save DataMove Detail: {}", dataMoveDetail);
    dataMoveRepository.save(dataMoveDetail);
  }

  private boolean checkJobFinished(DataMoveDetail dataMoveDetail) {
    return dataMoveDetail.getHistory().getStatus() != 0;
  }

  private boolean checkJobNeedRetry(DataMoveDetail dataMoveDetail) {
    boolean retryFlag = optimizeErrorLog(dataMoveDetail);
    switch (DataMoveJobType.idOf(dataMoveDetail.getHistory().getType())) {
      case TD2HD:
      case LC2HD:
        return retryFlag && doRetry(dataMoveDetail);
      case VDM2HD:
      case VDMVIEW2HD:
        return false;
    }
    return false;
  }

  public void move(List<DataMoveDetail> dataMoveDetailList) {
    LOGGER.info("{} Execute method with configured executor - {} ", this, Thread.currentThread().getName());
    LOGGER.info("Running User: {}", RUNNING_USERS);
    String RUNNING_KEY = String.format("%s-%s",
        dataMoveDetailList.get(0).getHistory().getNt(),
        dataMoveDetailList.get(0).getHistory().getType());
    if (RUNNING_USERS.contains(RUNNING_KEY)) {
      LOGGER.info("User {} is pending!", RUNNING_KEY);
      return;
    }
    RUNNING_USERS.add(RUNNING_KEY);
    for (DataMoveDetail dataMoveDetail : dataMoveDetailList) {
      if (FINISHED_JOBS.contains(dataMoveDetail.getHistory().getHistoryId())) {
        LOGGER.info("Finished Job Id: {}", FINISHED_JOBS);
        LOGGER.info("Job {} is pending!", dataMoveDetail.getHistory().getHistoryId());
        continue;
      }
      try {
        dataMoveDetail.getHistory().setStartTime(Objects.isNull(dataMoveDetail.getHistory().getStartTime())
            ? new Date() : dataMoveDetail.getHistory().getStartTime());
        initialize(dataMoveDetail);
        startMove(dataMoveDetail);
      } catch (Exception e) {
        LOGGER.error("DataMove Failed!", e);
        dataMoveDetail.getHistory().setStatus(2);
        dataMoveDetail.setErrorLog(e.getMessage());
      } finally {
        checkResult(dataMoveDetail);
      }
    }
    RUNNING_USERS.remove(RUNNING_KEY);
  }

  protected String getSparkSQL(DataMoveDetail dataMoveDetail) {
    return Optional.ofNullable(dataMoveDetail.getDdl()).orElse("") +
        Optional.ofNullable(dataMoveDetail.getSparkSql()).orElse("");
  }

  private boolean doRetry(DataMoveDetail dataMoveDetail) {
    int retryNum = RETRY_NUMS.getOrDefault(dataMoveDetail.getId(), 0);
    LOGGER.info("{} Take Retry Action For {} times", dataMoveDetail, retryNum);
    if (retryNum > 2) {
      RETRY_NUMS.remove(dataMoveDetail.getId());
      return false;
    } else {
      RETRY_NUMS.put(dataMoveDetail.getId(), retryNum + 1);
      resetJob(dataMoveDetail);
      return true;
    }
  }

  private void resetJob(DataMoveDetail dataMoveDetail) {
    dataMoveDetail.getHistory().setStatus(0);
    if (dataMoveDetail.getStep() == 2) {
      dataMoveDetail.setTaskId(0);
    }
    LOGGER.info("{} Reset Job", dataMoveDetail);
  }

  public boolean optimizeErrorLog(DataMoveDetail dataMoveDetail) {
    boolean retryFlag = false;
    if (dataMoveDetail.getHistory().getStatus() == 2 &&
        Objects.isNull(dataMoveDetail.getHistory().getLog()) &&
        Objects.nonNull(dataMoveDetail.getErrorLog())) {
      LOGGER.info("{} Optimize Log [{}]", dataMoveDetail, dataMoveDetail.getErrorLog());
      Optional<String> errorKey = logMap.keySet().stream().filter(
          msg -> dataMoveDetail.getErrorLog().contains(msg)).findFirst();
      String type = errorKey.isPresent() ? logMap.get(errorKey.get()) : "OTHERS";
      LOGGER.info("{} ERROR Log Type [{}]", dataMoveDetail, type);
      if (ErrorOptimizations.RESOURCEERROR.name().equals(type)
          || ErrorOptimizations.TDBGERROR.name().equals(type)
          || ErrorOptimizations.OTHERS.name().equals(type)) {
        retryFlag = true;
      }
      try {
        dataMoveDetail.getHistory().setLog(
            ErrorOptimizations.valueOf(type).getOptimizedLog(dataMoveDetail));
      } catch (PathNotFoundException e) {
        dataMoveDetail.getHistory().setLog(dataMoveDetail.getErrorLog());
        if (dataMoveDetail.getErrorLog().toLowerCase().contains("restclientexception")) {
          closeDatamoveSession(dataMoveDetail);
        }
      }
    }
    LOGGER.info("{} {} to Retry", dataMoveDetail, retryFlag ? "Need" : "No Need");
    return retryFlag;
  }

  private void closeDatamoveSession(DataMoveDetail dataMoveDetail) {
    String nt = dataMoveDetail.getHistory().getNt();
    Platform platform = Platform.valueOf(dataMoveDetail.getHistory().getTargetPlatform().trim());
    String notebookId = InterpreterManager.genOnetimeNoteId(nt, platform.getId());
    hadoopTableOperation.closeInterpreterSession(nt, notebookId);
  }

  protected enum ErrorOptimizations {
    TDPWDERROR {
      @Override
      public String getOptimizedLog(DataMoveDetail dataMoveDetail) {
        return "The tetadata password is not correct. Please reset it in User Settings Page!";
      }
    },
    TDBACCGERROR {
      @Override
      public String getOptimizedLog(DataMoveDetail dataMoveDetail) {
        return "You don't have TD Bridge access on Mozart. " +
            "Please follow below wiki to apply access OR use Hopper!" +
            "\nhttps://wiki.vip.corp.ebay.com/display/ND/Zeta+Data+Move+QA";
      }
    },
    TDBGERROR {
      @Override
      public String getOptimizedLog(DataMoveDetail dataMoveDetail) {
        int startIndex = dataMoveDetail.getErrorLog().indexOf("SQLException");
        int lastIndex = dataMoveDetail.getErrorLog().indexOf("\n", startIndex);
        return "TD Bridge is failed. Please check error log " +
            dataMoveDetail.getErrorLog().substring(startIndex + 12,
                lastIndex == -1 ? dataMoveDetail.getErrorLog().length() : lastIndex);
      }
    },
    RESOURCEERROR {
      @Override
      public String getOptimizedLog(DataMoveDetail dataMoveDetail) {
        return "Queue " + dataMoveDetail.getQueue() + " don't have enough resources, Please try later!";
      }
    },
    OTHERS {
      @Override
      public String getOptimizedLog(DataMoveDetail dataMoveDetail) {
        return JsonPath.read(dataMoveDetail.getErrorLog(), "$['main_cause']");

      }

    };

    public abstract String getOptimizedLog(DataMoveDetail dataMoveDetail);
  }


  protected DataMoveDetail runStepOne(DataMoveDetail dataMoveDetail) {

    if (dataMoveDetail.getHistory().getStatus() == 0 && dataMoveDetail.getStep() == 1) {
      LOGGER.info("{} Move Step 1", dataMoveDetail);
      String nt = dataMoveDetail.getHistory().getNt();
      JobResult jobResult = new JobResult();
      Platform platform = Platform.valueOf(dataMoveDetail.getHistory().getTargetPlatform().trim());
      if (platform.getId() != Platform.hermes.getId()) {
        // set queue info
        try {
          String queue = hadoopTableOperation.getUserQueue(nt, platform.getId());
          dataMoveDetail.setQueue(queue);
          LOGGER.info("Set User {} queue {}", nt, queue);
          jobResult = hadoopTableOperation.checkTableExist(platform
              , dataMoveDetail.getHistory().getTargetTable(), nt
              , InterpreterManager.genOnetimeNoteId(nt, platform.getId()));
        } catch (ClusterAccessDeniedException e) {
          dataMoveDetail.getHistory().setStatus(2);
          dataMoveDetail.getHistory().setLog(e.getMessage());
          return dataMoveDetail;
        }
      }

      LOGGER.info("{} Step 1 Execute Result [{}]", dataMoveDetail, jobResult);
      if (jobResult.getStatus() && dataMoveDetail.getIsDrop() == 0) {
        // table exist
        dataMoveDetail.getHistory().setStatus(2);
        dataMoveDetail.getHistory().setLog("Table is existed, you need choose override!");
      } else {
        /*
        if (jobResult.getStatus() && dataMoveDetail.isConvert()) {
          dataMoveDetail.setDdl(null);
        }*/
        dataMoveDetail.setStep(2);
      }
    }
    return dataMoveDetail;
  }

}
