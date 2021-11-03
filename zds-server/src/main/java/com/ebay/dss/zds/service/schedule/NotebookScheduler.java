package com.ebay.dss.zds.service.schedule;

import com.ebay.dss.zds.common.JsonUtil;
import com.ebay.dss.zds.common.SchedulerQueueDestination;
import com.ebay.dss.zds.dao.ZetaNotebookRepository;
import com.ebay.dss.zds.exception.ScheduleException;
import com.ebay.dss.zds.interpreter.InterpreterType;
import com.ebay.dss.zds.interpreter.interpreters.Interpreter;
import com.ebay.dss.zds.interpreter.output.result.JsonResult;
import com.ebay.dss.zds.magic.DynamicVariableHandler;
import com.ebay.dss.zds.model.*;
import com.ebay.dss.zds.model.schedule.ScheduleHistory;
import com.ebay.dss.zds.model.schedule.ScheduleJob;
import com.ebay.dss.zds.service.ZetaNotebookService;
import com.ebay.dss.zds.websocket.notebook.dto.DisconnectReq;
import com.ebay.dss.zds.websocket.notebook.dto.ExecuteCodeReq;
import com.google.common.base.Stopwatch;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.ebay.dss.zds.model.schedule.ScheduleHistory.JobRunStatus.*;
import static com.ebay.dss.zds.service.schedule.ScheduleConstant.*;


@Component
public class NotebookScheduler extends Scheduler {

  private static final Logger LOGGER = LoggerFactory.getLogger(NotebookScheduler.class);

  @Autowired
  NotebookUtilService noteService;

  @Autowired
  private ZetaNotebookService zetaNotebookService;

  @Autowired
  private ZetaNotebookRepository zetaNotebookRepository;

  @Autowired
  private DynamicVariableHandler dynamicVariableHandler;

  @Autowired
  private ScheduleJobService scheduleJobService;


  void startJob(ScheduleHistory scheduleHistory) {
    ScheduleJob scheduleJob = scheduleHistory.getScheduleJob();
    LOGGER.info("Thread [{}],Start to handle Notebook {}",
        Thread.currentThread().getName(), scheduleJob);
    scheduleHistory.setRunTime(new Date());
    scheduleHistoryRepository.save(scheduleHistory);
    NoteBookSchedulerJob noteBookSchedulerJob = JsonUtil.fromJson(
        scheduleJob.getTask(), NoteBookSchedulerJob.class);
    ExecuteCodeReq req = noteBookSchedulerJob.getReq();
    String noteId = req.getNotebookId();
    ZetaNotebook note = getScheduleNote(noteId);
    resetExecuteCodeReq(req, note);
    // setup interpreter
    String className = InterpreterType.toClass(req.getInterpreter()).getName();
    LOGGER.info("{} Get {} Configuration", scheduleJob, req.getInterpreter());
    Properties properties = noteService.getInterpreterProperties(scheduleJob.getNt(), noteBookSchedulerJob
        , req, noteId, className);
    properties = noteService.setSchedulerInfo(properties, scheduleHistory);
    LOGGER.info("{} Get Interpreter Properties: {}", scheduleJob, properties);
    // start to run
    Stopwatch stopwatch = Stopwatch.createStarted();
    Interpreter interpreter = noteService.openInterpreter(scheduleJob.getNt(), noteId, className, properties);
    LOGGER.info("Note {} OpenInterpreter Cost: [{}s]", noteId, stopwatch.elapsed(TimeUnit.SECONDS));
    LOGGER.info("{} Start to Run", scheduleJob);
    zetaNotebookService.executeCode(createQueueDestination(scheduleHistory), req);
    LOGGER.info("{} Execute Done", scheduleHistory.getScheduleJob());
    finishJob(scheduleHistory);
  }

  private ZetaNotebook getScheduleNote(String noteId) {
    try {
      return zetaNotebookRepository.getNotebook(noteId);
    } catch (EmptyResultDataAccessException e) {
      throw new ScheduleException(SCHEDULER_NOTE_NOT_EXIST);
    }
  }

  private SchedulerQueueDestination createQueueDestination(ScheduleHistory scheduleHistory) {
    return new SchedulerQueueDestination(scheduleHistory.getScheduleJob().getNt(),
        null, null, scheduleHistory);
  }

  private void resetExecuteCodeReq(ExecuteCodeReq req, ZetaNotebook note) {
    req.setNotebookId(note.getId());
    String noteContent = dynamicVariableHandler.isEnabled() ?
        note.getContent() : scheduleJobService.parseNotebookContent(note);
    if (StringUtils.isBlank(noteContent)) {
      throw new ScheduleException(SCHEDULER_NOTE_EMPTY);
    }
    req.setCodes(noteContent);
    // disable dag
    req.setProp(new HashMap<>());
    req.getProp().put("dag.enabled", "false");
  }

  @Override
  ScheduleHistory initJob(ScheduleHistory scheduleHistory) {
    if (Optional.ofNullable(super.initJob(scheduleHistory)).isPresent()) {
      return isDependencyReady(scheduleHistory) ? scheduleHistory : null;
    }
    return null;
  }

  private boolean isDependencyReady(ScheduleHistory scheduleHistory) {
    LOGGER.info("Check Schedule job instance {} dependency table is ready", scheduleHistory.getId());
    Map<String, Object> dependencyInfo = scheduleJobService.getDependencyStatusInfo(scheduleHistory);
    return dependencyInfo.isEmpty() || !dependencyInfo.containsValue(false);
  }


  void cancelJob(ScheduleHistory scheduleHistory) {
    LOGGER.info("[{}] start to close!", scheduleHistory);
    zetaNotebookService.doDisconnect(createQueueDestination(scheduleHistory),
        createDisconnectReq(scheduleHistory));
  }

  private DisconnectReq createDisconnectReq(ScheduleHistory scheduleHistory) {
    DisconnectReq req = new DisconnectReq();
    NoteBookSchedulerJob noteBookSchedulerJob = JsonUtil.fromJson(
        scheduleHistory.getScheduleJob().getTask(), NoteBookSchedulerJob.class);
    req.setNoteId(noteBookSchedulerJob.getReq().getNotebookId());
    req.setUserName(scheduleHistory.getScheduleJob().getNt());
    return req;
  }

  @Override
  public void finishJob(ScheduleHistory scheduleHistory) {
    LOGGER.info("Note Schedule Job is finished, status is {}",
        scheduleHistory.getJobRunStatus());
    if (scheduleHistory.getJobRunStatus() == SUCCESS) {
      try {
        LOGGER.info("Send Note Schedule job instance {} dependency table signal",
            scheduleHistory.getId());
        scheduleJobService.sendDependencyTableSignal(scheduleHistory);
      } catch (Exception e) {
        LOGGER.error("Call dependency signal api failed", e);
        throw new ScheduleException(DEPENDENCY_TABLE_SIGNAL_SEND_FAILED);
      }
    }
    super.finishJob(scheduleHistory);
  }

  String getJobFailLog(ScheduleHistory scheduleHistory) {
    if (Objects.isNull(scheduleHistory.getJobHistoryId())) {
      return Objects.isNull(scheduleHistory.getLog()) ? null :
          String.format(SCHEUDL_FAIL_LOG_MAIL_CONTENT, scheduleHistory.getLog());
    }
    List<Map<String, Object>> res = zetaNotebookRepository.getNotebookFailedHistoryByRequestId(
        scheduleHistory.getJobHistoryId().toString());
    if (!res.isEmpty()) {
      Map<String, Object> statement = res.get(0);
      JsonObject result = JsonResult
          .getParser()
          .parse((String) statement.get("result"))
          .getAsJsonObject();
      String content = result.get("result").getAsJsonArray().get(0).getAsJsonObject().get("content").getAsString();
      StringBuilder text = new StringBuilder();
      text.append(String.format(SCHEUDL_FAIL_SQL_MAIL_CONTENT, statement.getOrDefault("statement", "")));
      text.append(String.format(SCHEUDL_FAIL_LOG_MAIL_CONTENT, content));
      return text.toString();
    }
    return null;
  }
}
