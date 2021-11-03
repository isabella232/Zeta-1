package com.ebay.dss.zds.common;

import java.util.Date;
import java.util.stream.Collectors;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.ebay.dss.zds.model.schedule.ScheduleHistory;
import com.ebay.dss.zds.websocket.WebSocketResp;
import com.ebay.dss.zds.websocket.notebook.dto.ExecuteCodeError;
import com.ebay.dss.zds.websocket.notebook.dto.ExecuteCodeJob;
import com.ebay.dss.zds.websocket.notebook.dto.SessionExpiredMsg;

import static com.ebay.dss.zds.model.schedule.ScheduleHistory.JobRunStatus.FAIL;
import static com.ebay.dss.zds.model.schedule.ScheduleHistory.JobRunStatus.SUCCESS;

/**
 * Created by zhouhuang on 2018年11月5日
 */
public class SchedulerQueueDestination extends QueueDestination {

  private ScheduleHistory scheduleHistory;
  private static final Logger LOGGER = LoggerFactory.getLogger(SchedulerQueueDestination.class);

  public SchedulerQueueDestination(String userName, SimpMessagingTemplate template, String queue,
                                   ScheduleHistory scheduleHistory) {
    super(userName, template, queue);
    this.scheduleHistory = scheduleHistory;
  }

  @Override
  public <T> void sendData(WebSocketResp<T> payload) {
    String type = payload.getOp();
    if (WebSocketResp.OP.NB_CODE_INVALID_NOTEBOOK_STATUS.name().equals(type)
        || WebSocketResp.OP.NB_CODE_SESSION_EXPIRED.name().equals(type)
        || WebSocketResp.OP.NB_CODE_EXECUTE_ERROR.name().equals(type)) {
      T data = payload.getData();
      if (data instanceof ExecuteCodeError) {
        ExecuteCodeError error = (ExecuteCodeError) data;
        scheduleHistory.setLog(error.getErrors().stream().map(err -> optimizeLog(err))
            .collect(Collectors.joining(",")));
      } else if (data instanceof SessionExpiredMsg) {
        SessionExpiredMsg error = (SessionExpiredMsg) data;
        scheduleHistory.setLog(error.getReason());
      } else {
        scheduleHistory.setLog(optimizeLog(JsonUtil.toJson(data)));
      }
      scheduleHistory.setJobRunStatus(FAIL);
      scheduleHistory.setEndTime(new Date());
      LOGGER.error("{} Execute Failed", scheduleHistory);
    } else if (WebSocketResp.OP.NB_CODE_JOB_READY.name().equals(type)) {
      ExecuteCodeJob data = (ExecuteCodeJob) payload.getData();
      scheduleHistory.setJobHistoryId(Long.valueOf(data.getJobId()));
      LOGGER.info("{} Execute Ready", scheduleHistory);
    } else if (WebSocketResp.OP.NB_CODE_JOB_DONE.name().equals(type)) {
      scheduleHistory.setJobRunStatus(SUCCESS);
      scheduleHistory.setEndTime(new Date());
      LOGGER.info("{} Execute Done", scheduleHistory);
    }
  }

  public static String optimizeLog(String errorLog) {
    try {
      return JsonPath.read(errorLog, "$['main_cause']");
    } catch (PathNotFoundException e) {
      return errorLog;
    }
  }

  /**
   * @return the scheduleHistory
   */
  public ScheduleHistory getScheduleHistory() {
    return scheduleHistory;
  }

  /**
   * @param scheduleHistory the scheduleHistory to set
   */
  public void setScheduleHistory(ScheduleHistory scheduleHistory) {
    this.scheduleHistory = scheduleHistory;
  }

}
