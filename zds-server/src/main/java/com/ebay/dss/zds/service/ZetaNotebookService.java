package com.ebay.dss.zds.service;

import com.ebay.dss.zds.common.InterpreterRsp;
import com.ebay.dss.zds.common.QueueDestination;
import com.ebay.dss.zds.dao.ZetaNotebookRepository;
import com.ebay.dss.zds.dao.ZetaStatementRepository;
import com.ebay.dss.zds.exception.*;
import com.ebay.dss.zds.interpreter.InterpreterFactory;
import com.ebay.dss.zds.interpreter.InterpreterTask;
import com.ebay.dss.zds.interpreter.InterpreterType;
import com.ebay.dss.zds.interpreter.api.InterpreterService;
import com.ebay.dss.zds.magic.DynamicVariableHandler;
import com.ebay.dss.zds.magic.ParserUtils;
import com.ebay.dss.zds.message.EventTracker;
import com.ebay.dss.zds.message.ZetaEvent;
import com.ebay.dss.zds.message.event.Event;
import com.ebay.dss.zds.model.*;
import com.ebay.dss.zds.exception.ErrorCode;
import com.ebay.dss.zds.runner.ZetaNotebookJobRunner;
import com.ebay.dss.zds.service.schedule.ScheduleJobService;
import com.ebay.dss.zds.service.schedule.SchedulerCluster;
import com.ebay.dss.zds.state.StateManager;
import com.ebay.dss.zds.state.StateSnapshot;
import com.ebay.dss.zds.state.model.NotebookTabState;
import com.ebay.dss.zds.websocket.WebSocketResp;
import com.ebay.dss.zds.websocket.notebook.dto.*;
import com.google.common.collect.Lists;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.ebay.dss.zds.exception.ErrorCode.*;
import static com.ebay.dss.zds.message.event.Event.ZetaConnectionSocketSentEvent;
import static com.ebay.dss.zds.model.ZetaStatus.FAIL;

/**
 * Created by wenliu2 on 4/16/18.
 */
@Service
public class ZetaNotebookService {

  private static final Logger logger = LogManager.getLogger();
  private static final String NOTEBOOK_PATH_SEPARATOR = "/";
  private final ZetaNotebookRepository zetaNotebookRepository;
  private final ZetaStatementRepository zetaStatementRepository;
  private final ZetaNotebookJobRunner zetaNotebookJobRunner;
  private final InterpreterService interpreterService;
  private ScheduleJobService scheduleJobService;
  private StateManager stateManager;


  @Resource(name = "error-handle-rest-template")
  private RestTemplate restTemplate;

  @Autowired
  public ZetaNotebookService(ZetaNotebookRepository zetaNotebookRepository,
                             ZetaStatementRepository zetaStatementRepository,
                             ZetaNotebookJobRunner zetaNotebookJobRunner,
                             InterpreterService interpreterService,
                             ScheduleJobService scheduleJobService,
                             StateManager stateManager) {
    this.zetaNotebookRepository = zetaNotebookRepository;
    this.zetaStatementRepository = zetaStatementRepository;
    this.zetaNotebookJobRunner = zetaNotebookJobRunner;
    this.interpreterService = interpreterService;
    this.scheduleJobService = scheduleJobService;
    this.stateManager = stateManager;
  }

  public List<ZetaNotebook> getZetaNotebookBriefs(String nt) {
    List<ZetaNotebook> briefs = zetaNotebookRepository.getNotebookBriefsByNt(nt);
    return briefs.stream()
        .filter(n -> ZetaNotebook.NotebookType.sub_nb != n.getNbType())
        .collect(Collectors.toList());
  }

  public int deleteZetaNotebook(String noteId, String nt) {
    ZetaNotebook zetaNotebook = zetaNotebookRepository.getNotebook(noteId);
    int res;
    switch (zetaNotebook.getNbType()) {
      case single:
      case sub_nb:
        res = zetaNotebookRepository.deleteNotebookByIdAndNt(noteId, nt);
        break;
      case collection:
        res = zetaNotebookRepository.deleteNotebooksByCollectionIdAndNt(noteId, nt);
        res += zetaNotebookRepository.deleteNotebookByIdAndNt(noteId, nt);
        break;
      default:
        res = -1;
    }
    if (res > 0) {
      scheduleJobService.deleteScheduleJob(noteId, nt);
    }
    return res;
  }

  public ZetaNotebook stateOpenedAndGetNotebook(String noteId, String nt, int open) throws Exception {
    ZetaNotebook rawNotebook = zetaNotebookRepository.getNotebookByIdAndNt(noteId, nt, open);
    ZetaNotebook resNotebook;
    switch (rawNotebook.getNbType()) {
      case single:
      case sub_nb:
        resNotebook = rawNotebook;
        break;
      case collection:
        ZetaMultiNotebook multiNotebook = new ZetaMultiNotebook();
        ReflectionUtils.shallowCopyFieldState(rawNotebook, multiNotebook);
        List<ZetaNotebook> sub = zetaNotebookRepository.getNotebooksByCollectionIdAndNt(noteId, nt);
        multiNotebook.setSubNotebooks(sub);
        resNotebook = multiNotebook;
        break;
      default:
        throw new Exception("Notebook not exist!");
    }

    return resNotebook;
  }

  public List<ZetaNotebook> getOpenedZetaNotebooks(String nt) {
    List<ZetaNotebook> notebooks = zetaNotebookRepository.getOpenedNotebook(nt);
    MultiValueMap<String, ZetaNotebook> collectionIdMap = new LinkedMultiValueMap<>();
    List<ZetaNotebook> singleNotebooks = new ArrayList<>();
    List<ZetaMultiNotebook> multiNotebooks = new ArrayList<>();
    zetaNotebookRepository.getSubNotebooksBydNt(nt).forEach(n -> {
      collectionIdMap.add(n.getCollectionId(), n);
    });
    notebooks.forEach(n -> {

      switch (n.getNbType()) {
        case single:
          singleNotebooks.add(n);
          break;
        case sub_nb:
          collectionIdMap.add(n.getCollectionId(), n);
          break;
        case collection:
          ZetaMultiNotebook multiNotebook = new ZetaMultiNotebook();
          ReflectionUtils.shallowCopyFieldState(n, multiNotebook);
          multiNotebooks.add(multiNotebook);
          break;
      }
    });

    multiNotebooks.forEach(n -> {
      n.setSubNotebooks(collectionIdMap.getOrDefault(n.getId(), Collections.emptyList()));
    });
    singleNotebooks.addAll(multiNotebooks);
    return singleNotebooks;
  }

  public void updateNotebookSeqs(Map<String, Integer> seqMap, String nt) {
    seqMap.forEach((String nId, Integer seq) -> {
      zetaNotebookRepository.updateNotebookSeqById(nt, nId, seq);
    });
  }
  public ZetaNotebook getReadOnlyZetaNotebook(String id) {
    ZetaNotebook notebook = zetaNotebookRepository.getNotebook(id);
    // reset preference
    ZetaNotebookPreference preference = ZetaNotebookPreference.fromJson(notebook.getPreference());
    if (preference.variables != null) {
      preference.variables.clear();
    }
    if (preference.notebookConnection != null) {
      preference.notebookConnection.put("batchAccount", "");
    }
    preference.notebookProfile = "default";
    notebook.setPreference(preference.toJson());
    return notebook;
  }
  public List<Map<String, Object>> getMultiNotebookHistory(String notebookId) {
    List<Map<String, Object>> requests = zetaNotebookRepository.getMultiNotebookLastRequestByIdAndNt(notebookId);
    List<Map<String, Object>> subNotebooks = new ArrayList<>();
    requests.forEach(req -> {
      Long reqId = (Long) req.get("id");
      String noteId = (String) req.get("notebook_id");
      List<ZetaStatement> statements = zetaStatementRepository.getZetaStatementByRequest(reqId);
      Map<String, Object> subNoteHis = new HashMap<>();
      subNoteHis.put("jobId", reqId);
      subNoteHis.put("notebookId", noteId);
      subNoteHis.put("history", statements);
      subNotebooks.add(subNoteHis);
    });
    return subNotebooks;
  }

  private boolean checkCollectionAware(boolean isCollectionAware, ZetaNotebook notebook) {
    return isCollectionAware
        && notebook.getNbType() == ZetaNotebook.NotebookType.sub_nb
        && Objects.nonNull(notebook.getCollectionId());
  }

  public void executeCode(QueueDestination dest, ExecuteCodeReq req) {
    try {
      ZetaNotebook notebook = zetaNotebookRepository.getNotebook(req.getNotebookId());
      if (notebook == null) {
        throw new EntityNotFoundException("No ZetaNotebookJobRunner found with the given note id: " + req.getNotebookId());
      }

      if (checkCollectionAware(req.getIsCollectionAware(), notebook)) {
        zetaNotebookJobRunner.runCollectionAware(notebook, dest, req);
      } else {
        zetaNotebookJobRunner.run(notebook, dest, req);
      }
    } catch (Exception e) {
      throw wrap2InterpreterExecutionExceptionIfNot(INTERPRETER_CODE_EXECUTE_EXCEPTION, e, req.getNotebookId(), req.getReqId());
    }
  }

  public void cancelCode(QueueDestination dest, ExecuteCodeCancel req) {
    WebSocketResp<ExecuteCodeCancelRsp> rsp;
    try {
      interpreterService.cancelNote(req.getUserName(), req.getNotebookId());
      ExecuteCodeCancelRsp executeCodeCancelRsp = new ExecuteCodeCancelRsp(req.getNotebookId(),
          req.getJobId(), null,
          ExecuteCodeCancelRsp.STATUS.CANCELLED.name());
      executeCodeCancelRsp.setInfo("Note: " + req.getNotebookId() + " is cancelled");
      rsp = new WebSocketResp<>(WebSocketResp.OP.CANCEL_SUCCESS, executeCodeCancelRsp);
    } catch (Exception e) {
      throw wrap2InterpreterExecutionExceptionIfNot(INTERPRETER_CODE_EXECUTE_CANCEL_EXCEPTION, e, req.getNotebookId(), "");
    }
    dest.sendData(rsp);
  }

  public void doConnect(QueueDestination dest, ConnectionReq req) {
    try {
      checkNotebookThenConnect(dest, req);
    } catch (Exception e) {
      interpreterService.removeNote(req.getUserName(), req.getNoteId());
      throw wrap2InterpreterServiceExceptionIfNot(INTERPRETER_CONNECT_EXCEPTION, e, req.getNoteId());
    }
  }

  private void checkNotebookThenConnect(QueueDestination dest, ConnectionReq req) {
    ZetaNotebook notebook = zetaNotebookRepository.getNotebook(req.getNoteId());
    if (notebook == null) {
      throw new EntityNotFoundException("Notebook not found with the given id: " + req.getNoteId());
    }

    ZetaResponse<InterpreterRsp> response;
    if (checkCollectionAware(req.getIsCollectionAware(), notebook)) {
      response = interpreterService.openNote(req.getUserName(),
          notebook.getCollectionId(), req.getInterpreter(), req.getProp());
    } else {
      response = interpreterService.openNote(req.getUserName(),
          req.getNoteId(), req.getInterpreter(), req.getProp());
    }

    ConnectionRsp connectionRsp = new ConnectionRsp(req.getUserName(), req.getNoteId(), req.getInterpreter(), response);
    WebSocketResp<ConnectionRsp> webSocketResp = new WebSocketResp<>(WebSocketResp.OP.CONNECTION_SUCCESS, connectionRsp);
    dest.sendData(webSocketResp);
    EventTracker.postEvent(ZetaConnectionSocketSentEvent(
        req.getUserName(),
        req.getNoteId(),
        InterpreterType.toClass(req.getInterpreter()).getName(),
        webSocketResp));
  }

  public void doDisconnect(QueueDestination dest, DisconnectReq req) {
    ZetaEvent zdce = Event
        .ZetaDisconnectionEvent(req.getUserName(), req.getNoteId(), null);
    EventTracker.postEvent(zdce);
    try {

      interpreterService.removeNote(req.getUserName(), req.getNoteId());

      EventTracker.postEvent(Event
          .ZetaDisconnectionSuccessEvent(req.getUserName(), req.getNoteId(), null, zdce.getRecordTime()));

      DisconnectRsp disconnectRsp = new DisconnectRsp(
          req.getNoteId(), "Note: " + req.getNoteId() + " is disconnected", true);
      WebSocketResp<DisconnectRsp> webSocketResp = new WebSocketResp<>(
          WebSocketResp.OP.DISCONNECTION_SUCCESS, disconnectRsp);
      dest.sendData(webSocketResp);
    } catch (Exception e) {
      EventTracker.postEvent(Event
          .ZetaDisconnectionFailedEvent(req.getUserName(), req.getNoteId(), null, e.toString(), zdce.getRecordTime()));
      throw wrap2InterpreterServiceExceptionIfNot(INTERPRETER_DISCONNECT_EXCEPTION, e, req.getNoteId());
    }
  }

  public void doRecover(QueueDestination dest, RecoverReq req) {
    Map<String, Object> tabStateMap = getCurrentNoteRunningStatus(dest.getUserName(), req.getNoteId());
    RecoverRsp recoverRsp = new RecoverRsp(req.getNoteId(), tabStateMap);
    WebSocketResp<RecoverRsp> webSocketResp = new WebSocketResp<>(
            WebSocketResp.OP.RECOVER_TRIED, recoverRsp);
    dest.sendData(webSocketResp);
  }

  public List<String> deleteFolders(String nt, List<String> folders, boolean recursive) {
    List<String> idList = new ArrayList<>();
    if (Objects.isNull(folders)) {
      logger.debug("Folders are null, skip deletion...");
      return idList;
    }

    for (String folder : folders) {
      if (StringUtils.isBlank(folder) || StringUtils.equals(NOTEBOOK_PATH_SEPARATOR, folder)) {
        continue;
      }
      try {
        List<String> ids = deleteFolder(nt, folder, recursive);
        idList.addAll(ids);
      } catch (Exception e) {
        logger.error("delete folder error", e);
//        results.put(folder, e.getMessage());
      }
    }
    return idList;
  }
  private List<String> deleteFolder(String nt, String folder, boolean recursive) {
    if (recursive) {
      return deleteFoldersRecursive(nt, folder);
    } else {
      return deleteSingleFolder(nt, folder);
    }
  }
  private List<String> deleteSingleFolder(String nt, String folder) {
    List<String> idList = zetaNotebookRepository.getNotebookIdByPathAndNt(folder, nt);
    zetaNotebookRepository.deleteNotebooksByNtAndIds(idList, nt);
    return idList;
  }

  private List<String> deleteFoldersRecursive(String nt, String folder) {
    List<String> idList = zetaNotebookRepository.getNotebookIdRecursivelyByPathAndNt(folder, nt,  NOTEBOOK_PATH_SEPARATOR);
    zetaNotebookRepository.deleteNotebooksByNtAndIds(idList, nt);
    return idList;
  }

  private InterpreterExecutionException wrap2InterpreterExecutionExceptionIfNot(ErrorCode errorCode, Throwable e, String noteId, String reqId) {
    if (!(e instanceof InterpreterExecutionException)) {
      return new InterpreterExecutionException(errorCode, new ExecuteCodeError(noteId, reqId), e);
    } else {
      return (InterpreterExecutionException) e;
    }
  }

  private InterpreterServiceException wrap2InterpreterServiceExceptionIfNot(ErrorCode errorCode, Throwable e, String noteId) {
    if (!(e instanceof InterpreterServiceException)) {
      return new InterpreterServiceException(errorCode, FAIL, noteId, e);
    } else {
      return (InterpreterServiceException) e;
    }
  }

  // todo: we might need to add a lock here, as in another thread, the status message might arrive faster to client side than here
  public Map<String, Object> getCurrentNoteRunningStatus(String nt, String noteId) {

    Map<String, Object> status = new HashMap<>();
    try {
      status.put("connection", interpreterService.grepConnectionStatus(nt, noteId));
      Map<Integer, NotebookTabState> tabStatus = new HashMap<>();
      status.put("tab", tabStatus);
      StateSnapshot<InterpreterTask> stateSnapshot = stateManager.findStateSnapshot(InterpreterFactory.getGrpKey(nt, noteId));
      if (stateSnapshot == null) {
        logger.info("Got empty StateSnapshot of note: " + noteId);
        return status;
      }
      InterpreterTask task = stateSnapshot.unwrap();
      if (task == null) {
        logger.info("Got empty InterpreterTask of note: " + noteId);
        return status;
      }

      List<CodeWithSeq> codeWithSeqs = task.codeWithSeqs;
      for (CodeWithSeq codeWithSeq : codeWithSeqs) {
        String result = null;
        Long maybeStartDt = null;
        Long maybeEndDt = null;
        long now = System.currentTimeMillis();
        if (codeWithSeq.finished()) {
          ZetaStatement statement = zetaStatementRepository.getZetaStatement(codeWithSeq.getStatementId());
          result = statement.getResult();
          maybeStartDt = statement.getStartDt().getTime();
          maybeEndDt = statement.getUpdateDt().getTime();
        }

        ExecuteCodeResult executeCodeResult = new ExecuteCodeResult(task.noteId, task.jobId, task.reqId);
        executeCodeResult.setCode(codeWithSeq);
        executeCodeResult.setResult(result);
        executeCodeResult.setStartDt(maybeStartDt);
        executeCodeResult.setEndDt(maybeEndDt);

        NotebookTabState tabState = new NotebookTabState(executeCodeResult, now);
        tabStatus.put(codeWithSeq.getSeq(), tabState);
      }

    } catch (Exception ex) {
      logger.error("Error when get status: {}", ExceptionUtils.getFullStackTrace(ex));
    }

    return status;
  }

  public void cleanCurrentNoteStatus(String noteId) {
    stateManager.destroyStateSnapshot(noteId);
  }

  public void cleanCurrentNoteStatus(String noteId, long statementId) {
    StateSnapshot<InterpreterTask> stateSnapshot = stateManager.findStateSnapshot(noteId);
    if (stateSnapshot == null) {
      logger.info("Got empty status of note: " + noteId);
      return;
    }
    InterpreterTask task = stateSnapshot.unwrap();
    if (task == null) {
      logger.info("Got empty status of note: " + noteId);
      return;
    }
    task.codeWithSeqs.removeIf(codeWithSeq -> codeWithSeq.getStatementId() == statementId);
  }

  public String parseAndReplaceSQL(String noteId) {
    ZetaNotebook notebook = zetaNotebookRepository.getNotebook(noteId);
    return parseAndReplaceSQL(notebook);
  }

  public String parseAndReplaceSQL(ZetaNotebook notebook) {
    String codes = notebook.getContent();
    List<ParserUtils.SQLSegment> sqlList = new ParserUtils
            .SplitSQL(codes)
            .getSegments();
    NotebookVarsMap notebookVarsMap = NotebookVarsMap.fromNotebook(notebook);
    DynamicVariableHandler dynamicVariableHandler = zetaNotebookJobRunner.getDynamicVariableHandler();
    String injected = sqlList.stream().map(segment -> {
      CodeWithSeq codeWithSeq = new CodeWithSeq(segment.originalSQL, 0);
      codeWithSeq.setCommentPos(segment.commentPos);
      dynamicVariableHandler.handle(null, codeWithSeq, notebook, notebookVarsMap);
      return codeWithSeq.getCode();
    }).collect(Collectors.joining(";\n"));
    return injected;
  }

}
