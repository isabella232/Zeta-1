package com.ebay.dss.zds.runner;

import com.ebay.dss.zds.common.Constant;
import com.ebay.dss.zds.common.QueueDestination;
import com.ebay.dss.zds.magic.ParserUtils;
import com.ebay.dss.zds.dao.ZetaJobRequestRepository;
import com.ebay.dss.zds.exception.ErrorCode;
import com.ebay.dss.zds.exception.InterpreterExecutionException;
import com.ebay.dss.zds.exception.InvalidInterpreterTypeException;
import com.ebay.dss.zds.interpreter.*;
import com.ebay.dss.zds.interpreter.input.ExecutionContext;
import com.ebay.dss.zds.interpreter.interpreters.Interpreter;
import com.ebay.dss.zds.interpreter.interpreters.livy.LivyCodeKind;
import com.ebay.dss.zds.interpreter.interpreters.livy.ZLivySparkSqlInterpreter;
import com.ebay.dss.zds.interpreter.listener.InterpreterListener;
import com.ebay.dss.zds.interpreter.listener.InterpreterListenerFactory;
import com.ebay.dss.zds.interpreter.output.result.JsonResult;
import com.ebay.dss.zds.magic.DynamicVariableHandler;
import com.ebay.dss.zds.magic.HandleState;
import com.ebay.dss.zds.magic.HandleStateContext;
import com.ebay.dss.zds.magic.exception.ZetaMagicHandleException;
import com.ebay.dss.zds.magic.exception.ZetaVariableInjectionException;
import com.ebay.dss.zds.model.NotebookVarsMap;
import com.ebay.dss.zds.model.ZetaJobRequest;
import com.ebay.dss.zds.model.ZetaNotebook;
import com.ebay.dss.zds.model.ZetaNotebookPreference;
import com.ebay.dss.zds.service.DumpFileService;
import com.ebay.dss.zds.state.StateManager;
import com.ebay.dss.zds.state.annotation.StateSourcePrefer;
import com.ebay.dss.zds.state.source.InMemoryStateSource;
import com.ebay.dss.zds.websocket.WebSocketResp;
import com.ebay.dss.zds.websocket.notebook.dto.*;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.zeppelin.interpreter.InterpreterResult;
import org.apache.zeppelin.interpreter.InterpreterResultMessage;
import org.codehaus.plexus.util.ExceptionUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by wenliu2 on 4/2/18.
 */

@Component
@StateSourcePrefer(prefer = InMemoryStateSource.class)
public class ZetaNotebookJobRunner {
  public final static Logger logger = LoggerFactory.getLogger(ZetaNotebookJobRunner.class);
  private final ZetaJobRequestRepository zetaJobRequestRepository;
  private final ConfigurationManager configurationManager;
  private final InterpreterListenerFactory interpreterListenerFactory;
  private final DumpFileService dumpFileService;
  private final StateManager stateManager;
  private final DynamicVariableHandler dynamicVariableHandler;

  @Value("${spring.profiles.active}")
  private String profile;

  private InterpreterFactory factory;

  private InterpreterConfiguration intpConfig;

  @PostConstruct
  private void initFactory() {
    intpConfig = configurationManager.getDefaultConfiguration();
  }

  @Autowired
  public ZetaNotebookJobRunner(
          InterpreterFactory factory,
          ZetaJobRequestRepository zetaJobRequestRepository,
          ConfigurationManager configurationManager,
          InterpreterListenerFactory interpreterListenerFactory,
          DumpFileService dumpFileService,
          StateManager stateManager,
          DynamicVariableHandler dynamicVariableHandler) {
    this.factory = factory;
    this.zetaJobRequestRepository = zetaJobRequestRepository;
    this.configurationManager = configurationManager;
    this.interpreterListenerFactory = interpreterListenerFactory;
    this.dumpFileService = dumpFileService;
    this.stateManager = stateManager;
    this.dynamicVariableHandler = dynamicVariableHandler;
  }

  public enum Status {
    READY, RUNNING, DONE, FAIL
  }

  private String getDefaultLimit() {
    return intpConfig
            .getProperties()
            .getProperty(Constant.DEFAULT_LIMIT_KEY, "1000");
  }

  Integer getLimit(Interpreter interpreter) {
    return Integer.valueOf(interpreter.getProperties()
            .getProperty(Constant.VIEW_LIMIT, getDefaultLimit()));
  }

  ZetaJobRequest fillJobRequestContext(ZetaJobRequest zetaJobRequest, ZetaNotebook notebook, Interpreter interpreter) {
    try {
      if (StringUtils.isNotEmpty(notebook.getPreference())) {
        ZetaNotebookPreference preference = ZetaNotebookPreference.fromJson(notebook.getPreference());
        if (preference.notebookConnection != null) {
          zetaJobRequest.fillRequestContext(preference, interpreter);
        } else {
          logger.warn("No any notebookConnection found under user: {}, notebook: {}", notebook.getNt(), notebook.getId());
        }
      } else {
        logger.warn("No any preference found under user: {}, notebook: {}", notebook.getNt(), notebook.getId());
      }
    } catch (Exception ex) {
      logger.error("Error when fill job request context, nt: {}, notebook: {}", notebook.getNt(), notebook.getId());
    }
    return zetaJobRequest;
  }

  ExecuteCodeError handleFindInterpreterError(Exception e, QueueDestination dest, ExecuteCodeReq req, String jobId) {
    logger.error(e.getMessage());
    ExecuteCodeError error = new ExecuteCodeError(req.getNotebookId(), req.getReqId());
    error.setJobId(jobId);
    error.setCode(new CodeWithSeq("", 0));
    error.addError(e.getMessage());
    return error;
  }

  ExecuteCodeError handleInterpreterRunningConflict(ZetaNotebook notebook, QueueDestination dest, ExecuteCodeReq req) {
    logger.warn("There is a job running for this notebook (note name: {})", notebook.getTitle());
    ExecuteCodeError error = new ExecuteCodeError(req.getNotebookId(), req.getReqId());
    error.addError("ZetaNotebookJobRunner is running.");
    return error;
  }

  ExecutionContext prepareExecutionContext(ZetaNotebook notebook, CodeWithSeq codeWithSeq, ExecuteCodeReq req, String jobId) {
    ExecutionContext ec = new ExecutionContext(notebook.getId(), codeWithSeq.getSeq());
    ec.setCode(codeWithSeq.getCode());
    ec.setJobId(jobId);
    ec.setRequestId(req.getReqId());

    if (Objects.nonNull(req.getProp())) {
      Properties properties = new Properties();
      properties.putAll(req.getProp());
      ec.setProp(properties);
    }

    if (req instanceof ExecuteDumpReq) {
      logger.info("Got dump request in job runner");
      ec.setProperty(Interpreter.OperationType.OP_KEY, Interpreter.OperationType.DUMP);
      int limit = ((ExecuteDumpReq) req).getLimit();
      if (limit > 0) {
        ec.setProperty(Constant.DUMP_LIMIT, String.valueOf(limit));
      }
      InterpreterType.EnumType type = InterpreterType.fromString(req.getInterpreter());
      // todo: refactor livy interpreter dump
    }
    return ec;
  }

  private NotebookVarsMap tryGetNotebookVars(ZetaNotebook notebook, Map<String, String> properties) {
    try {
      String varsMapStr = Optional.ofNullable(properties)
              .map((Map<String, String> p) -> properties.get(NotebookVarsMap.PROP_KEY)).orElse(null);
      if (StringUtils.isNotEmpty(varsMapStr)) {
        // we trust the vars from the frontend even thought it might not synced with the backend
        logger.info("note: {}, nt: {} is using vars: {}", notebook.getId(), notebook.getNt(), varsMapStr);
        return NotebookVarsMap.fromJson(varsMapStr);
      } else {
        // not vars pass from the frontend let's check if the backend records the vars;
        logger.info("{}, note: {}, nt: {}", "not vars pass from the frontend let's check if the backend records the vars",
                notebook.getId(), notebook.getNt());
        return NotebookVarsMap.fromNotebook(notebook);
      }
    } catch (Exception ex) {
      logger.error("Failed to get the variables: " + ExceptionUtils.getFullStackTrace(ex));
      return null;
    }
  }

  public void run(ZetaNotebook notebook,
                  QueueDestination dest,
                  ExecuteCodeReq req,
                  @NotNull Interpreter interpreter) {

    InterpreterType.EnumType interpreterType = InterpreterType.fromString(req.getInterpreter());

    if (Objects.equals(notebook.getStatus(), Status.RUNNING.name())) {
      ExecuteCodeError error = handleInterpreterRunningConflict(notebook, dest, req);
      throw new InterpreterExecutionException(ErrorCode.INTERPRETER_INVALID_NOTEBOOK_STATUS_EXCEPTION, error);
    }

    String codes = Strings.nullToEmpty(req.getCodes()).trim();
    if (Strings.isNullOrEmpty(codes)) {
      //do nothing for empty sqls
      return;
    }

    String jobId = createJob(dest, notebook, req, interpreter);

    List<CodeWithSeq> codeList = split(dest, req, jobId, interpreterType);
    NotebookVarsMap someVarsMap = tryGetNotebookVars(notebook, req.getProp());

    stateManager.runWithState(new InterpreterTask(notebook.getId(), jobId, req.getReqId(), codeList) {

      private final String grpKey = InterpreterFactory.getGrpKey(dest.getUserName(), notebook.getId());
      @Override
      public void run() {
        //interpreter.exe
        boolean failed = false;
        logger.info("Start to execute sql with user: {} notebookId: {} interpreter: [user:{}, group:{}]"
                , dest.getUserName(), req.getNotebookId(), interpreter.getUserName(), interpreter.getInterpreterGroup().getGroupId());
        interpret_loop:
        for (CodeWithSeq codeWithSeq : codeList) {
          /*
           * The interpreter can be paused by other or current thread,
           * such as the user send out the cancel request when the job is running.
           * So the interpreter should be marked as paused and aboard the rest paragraph
           * and after this pause the interpreter shouldn't pause next time
           * */
          if (interpreter.checkPaused()) {
            logger.info("Current interpreter is paused!");
            //todo:Maybe need to do something to make the transaction flow reasonable
            break;
          }

          HandleStateContext handleStateContext = preHandleOrThrow(
                  interpreter, req, jobId, codeWithSeq, notebook, someVarsMap);

          InterpreterListener listener = interpreterListenerFactory.getInstance(notebook, interpreter,
                  jobId, codeWithSeq, req, dest);
          ExecutionContext ec = prepareExecutionContext(notebook, codeWithSeq, req, jobId);

          codeWithSeq.setStatus(CodeWithSeq.Status.RUNNING);
          InterpreterResult result = handleStateContext.executionAgent.execute(ec, listener);

          InterpreterResult.Code code = result.code();
          switch (code) {
            case SUCCESS:
              codeWithSeq.setStatus(CodeWithSeq.Status.DONE);
              JsonResult jsonResult = new JsonResult(result);
              handleSuccess(interpreter, jsonResult, dest, req, jobId, codeWithSeq, listener);
              callbackIfMagicStatement(handleStateContext, jsonResult, dest, notebook);
              break;
            case CLOSED:
              logger.info("Result and remaining interpretation discarded, " +
                      "maybe the interpreter is already closed by user");
              codeWithSeq.setStatus(CodeWithSeq.Status.FAIL);
              failed = true;
              break interpret_loop;
            default:
              codeWithSeq.setStatus(CodeWithSeq.Status.FAIL);
              ExecuteCodeError error = handleNotSuccess(dest, req, jobId, codeWithSeq, result);
              throw new InterpreterExecutionException(ErrorCode.INTERPRETER_CODE_EXECUTE_EXCEPTION, error);
          }
        }

        logger.info("Finished");
        if (!failed) {
          handleJobDoneWithoutFailure(dest, req, jobId);
        }
      }

      @Override
      public String recoverKey() {
        return grpKey;
      }
    }, false);
  }

  Interpreter findInterpreter(ZetaNotebook notebook,
                              QueueDestination dest,
                              ExecuteCodeReq req,
                              boolean findCollectionLevelInterpreter) {
    InterpreterType.EnumType interpreterType = InterpreterType.fromString(req.getInterpreter());
    String notebookId = req.getNotebookId();
    if (findCollectionLevelInterpreter
            && notebook.getNbType() == ZetaNotebook.NotebookType.sub_nb
            && Objects.nonNull(notebook.getCollectionId())) {
      logger.info("current notebook is a sub notebook of " + notebook.getCollectionId());
      notebookId = notebook.getCollectionId();
    }
    return factory.findInterpreter(dest.getUserName(),
            notebookId, interpreterType.getInterpreterClass().getName());
  }

  Interpreter findInterpreterOrThrow(ZetaNotebook notebook,
                                     QueueDestination dest,
                                     ExecuteCodeReq req,
                                     boolean findCollectionLevelInterpreter) {
    try {
      return findInterpreter(notebook, dest, req, findCollectionLevelInterpreter);
    } catch (Exception e) {
      ExecuteCodeError error = handleFindInterpreterError(e, dest, req, "");
      throw new InterpreterExecutionException(ErrorCode.INTERPRETER_SESSION_EXPIRED_EXCEPTION, error);
    }
  }

  public void runCollectionAware(ZetaNotebook notebook, QueueDestination dest, ExecuteCodeReq req) {
    run(notebook, dest, req, true);
  }

  public void run(ZetaNotebook notebook, QueueDestination dest, ExecuteCodeReq req) {
    run(notebook, dest, req, false);
  }

  public void run(ZetaNotebook notebook, QueueDestination dest, ExecuteCodeReq req, boolean findCollectionLevelInterpreter) {
    Interpreter interpreter = findInterpreterOrThrow(notebook, dest, req, findCollectionLevelInterpreter);
    run(notebook, dest, req, interpreter);
  }

  void handleJobDoneWithoutFailure(QueueDestination dest, ExecuteCodeReq req, String jobId) {
    WebSocketResp.OP op = req instanceof ExecuteDumpReq ?
            WebSocketResp.OP.NB_CODE_DUMP_DONE :
            WebSocketResp.OP.NB_CODE_JOB_DONE;
    WebSocketResp<ExecuteCodeJob> jobDone = new WebSocketResp<>(op, new ExecuteCodeJob(req.getNotebookId(), jobId, req.getReqId()));
    dest.sendData(jobDone);
  }

  void handleSuccess(Interpreter interpreter,
                     JsonResult result,
                     QueueDestination dest,
                     ExecuteCodeReq req,
                     String jobId,
                     CodeWithSeq codeWithSeq,
                     InterpreterListener listener) {

    String jsonString = result.get();
    ExecuteCodeResult executeCodeResult = new ExecuteCodeResult(req.getNotebookId(), jobId, req.getReqId());
    executeCodeResult.setCode(codeWithSeq);

    executeCodeResult.setResult(jsonString);
    executeCodeResult.setStartDt(
            listener.getStartDt() == null ? null
                    : listener.getStartDt().getMillis());
    executeCodeResult.setEndDt(
            listener.getEndDt() == null ? new DateTime().getMillis()
                    : listener.getEndDt().getMillis()
    );
    WebSocketResp<ExecuteCodeResult> resp = new WebSocketResp<>(WebSocketResp.OP.NB_CODE_STATEMENT_SUCCESS, executeCodeResult);
    dest.sendData(resp);
  }

  void callbackIfMagicStatement(HandleStateContext handleStateContext,
                                JsonResult successResult,
                                QueueDestination dest,
                                ZetaNotebook notebook) {
    if (handleStateContext.handleState == HandleState.MAGIC) {
      try {
        handleStateContext.callback.onSuccess(successResult, dest, notebook);
      } catch (Exception ex) {
        NotebookVarsRefreshFailed refreshFailed = new NotebookVarsRefreshFailed(notebook.getId(), ex.getMessage());
        WebSocketResp<NotebookVarsRefreshFailed> resp =
                new WebSocketResp<>(WebSocketResp.OP.NB_VAR_REFRESH_FAILED, refreshFailed);
        dest.sendData(resp);
      }
    } else {
      logger.info("This is not magic statement, no need to handle callback");
    }
  }

  ExecuteCodeError handleNotSuccess(
          QueueDestination dest,
          ExecuteCodeReq req,
          String jobId,
          CodeWithSeq sqlWithSeq,
          InterpreterResult result) {

    ExecuteCodeError error = new ExecuteCodeError(req.getNotebookId(), req.getReqId());
    error.setJobId(jobId);
    error.setCode(sqlWithSeq);

    InterpreterResult.Code code = result.code();

    if (code == InterpreterResult.Code.ERROR) {
      for (InterpreterResultMessage message : result.message()) {
        error.addError(message.getData());
      }
      //success
    } else {
      String msg = "Invalid error code: " + code.name();
      logger.error(msg);
      error.addError(msg);
    }

    return error;
  }

  HandleStateContext preHandleOrThrow(Interpreter interpreter,
                                      ExecuteCodeReq req,
                                      String jobId,
                                      CodeWithSeq codeWithSeq,
                                      ZetaNotebook notebook,
                                      NotebookVarsMap someVarsMap)
          throws InterpreterExecutionException {
    try {
      return dynamicVariableHandler.handle(interpreter, codeWithSeq, notebook, someVarsMap);
    } catch (ZetaMagicHandleException zhe) {
      ExecuteCodeError error = handleMagicStatementFailure(req, jobId, codeWithSeq, zhe);
      throw new InterpreterExecutionException(ErrorCode.INTERPRETER_CODE_EXECUTE_EXCEPTION, error);
    } catch (ZetaVariableInjectionException zje) {
      ExecuteCodeError error = handleVariableInjectionFailure(req, jobId, codeWithSeq, zje);
      throw new InterpreterExecutionException(ErrorCode.INTERPRETER_CODE_EXECUTE_EXCEPTION, error);
    }
  }

  ExecuteCodeError handleMagicStatementFailure(
          ExecuteCodeReq req,
          String jobId,
          CodeWithSeq sqlWithSeq,
          ZetaMagicHandleException zhe) {
    return handleDynamicVariableFailure(req, jobId, sqlWithSeq, zhe);
  }

  ExecuteCodeError handleVariableInjectionFailure(
          ExecuteCodeReq req,
          String jobId,
          CodeWithSeq sqlWithSeq,
          ZetaVariableInjectionException zje) {
    return handleDynamicVariableFailure(req, jobId, sqlWithSeq, zje);
  }

  ExecuteCodeError handleDynamicVariableFailure(
          ExecuteCodeReq req,
          String jobId,
          CodeWithSeq sqlWithSeq,
          Exception zhe) {

    ExecuteCodeError error = new ExecuteCodeError(req.getNotebookId(), req.getReqId());
    error.setJobId(jobId);
    error.setCode(sqlWithSeq);
    error.addError(zhe.getMessage());
    return error;
  }

  List<CodeWithSeq> split(QueueDestination dest, ExecuteCodeReq req, String jobId, InterpreterType.EnumType type) {
    List<CodeWithSeq> codeList = null;
    switch (type) {
      case LIVY_SPARK:
        codeList = splitCode(req);
        break;
      case LIVY_SPARKSQL:
      case JDBC:
        codeList = splitSQLV2(req);
        break;
      case SSH_RESTRICTED_HDFS:
      case LIVY_PYSPARK:
      case LIVY_SPARKR:
        codeList = rawCodeOrSql(req);
        break;
      case LIVY_SHARED:
        codeList = splitOfLivyShared(req);
        break;
      case IMITATE:
        codeList = splitCode(req);
        break;
      default:
        throw new InvalidInterpreterTypeException("Invalid interpreter type : " + type.getName());
    }
    ExecuteCodePreprocessed reply = new ExecuteCodePreprocessed(req.getNotebookId(), jobId, req.getReqId(), codeList);
    dest.sendData(WebSocketResp.get(WebSocketResp.OP.NB_CODE_PREPROCESSED, reply));
    return codeList;
  }

  private List<CodeWithSeq> splitOfLivyShared(ExecuteCodeReq req) {
    LivyCodeKind codeKind = LivyCodeKind.valueOf(req.getProperty(Constant.SHARED_LIVY_CODE_TYPE));
    switch (codeKind) {
      case sql:
        return splitSQLV2(req);
      case pyspark:
      case sparkr:
      case spark:
        return rawCodeOrSql(req);
      default:
        throw new InvalidInterpreterTypeException("Invalid interpreter type : " + codeKind);
    }
  }

  List<CodeWithSeq> rawCodeOrSql(ExecuteCodeReq req) {
    return Collections.singletonList(new CodeWithSeq(req.getCodes(), 0));
  }

  List<CodeWithSeq> splitCode(ExecuteCodeReq req) {
    ArrayList<CodeWithSeq> codeList = new ArrayList<>();
    String fullCode = req.getCodes();
    fullCode = fullCode.replaceAll("(/\\*{1,2}[\\s\\S]*?\\*/)|(//.*[^\\n]*)", "");
    codeList.add(new CodeWithSeq(fullCode, 0));
    return codeList;
  }

  @Deprecated
  List<CodeWithSeq> splitSQL(ExecuteCodeReq req) {
    String fullSQL = req.getCodes();

    fullSQL = fullSQL.replaceAll("(/\\*{1,2}[\\s\\S]*?\\*/)|(--.*[^\\n]*)", "");
    Pattern semiColonNotWithinQuotes = Pattern.compile(";(?=(?:[^\']*\'[^\']*\')*[^\']*$)");

    Iterable<String> sqlIt = Splitter.on(semiColonNotWithinQuotes)
            .trimResults()
            .omitEmptyStrings()
            .split(fullSQL);
    List<String> sqlList = Lists.newArrayList(sqlIt);

    return IntStream.range(0, sqlList.size())
            .mapToObj((int idx) -> new CodeWithSeq(sqlList.get(idx), idx))
            .collect(Collectors.toList());
  }

  List<CodeWithSeq> splitSQLV2(ExecuteCodeReq req) {
    String fullSQL = req.getCodes();
    return splitSQLV2(fullSQL);
  }

  public List<CodeWithSeq> splitSQLV2(String fullSQL) {
    // fullSQL = fullSQL.replaceAll("(/\\*{1,2}[\\s\\S]*?\\*/)|(--.*[^\\n]*)", "");
    List<ParserUtils.SQLSegment> sqlList = new ParserUtils
            .SplitSQL(fullSQL)
            .trimResult()
            .omitEmptyStrings()
            .removeComment()
            .getSegments();

    return IntStream.range(0, sqlList.size())
            .mapToObj((int idx) -> {
              ParserUtils.SQLSegment segment = sqlList.get(idx);
              CodeWithSeq codeWithSeq = new CodeWithSeq(segment.handledSQL, idx);
              if (!segment.splitContext.removeComment) {
                codeWithSeq.setCommentPos(segment.commentPos);
              }
              return codeWithSeq;
            })
            .collect(Collectors.toList());
  }

  private String createJob(QueueDestination dest, ZetaNotebook notebook, ExecuteCodeReq req, @NotNull Interpreter interpreter) {
    //TODO: insert to DB and get JobId
    ZetaJobRequest jobRequest = new ZetaJobRequest();
    jobRequest.setNotebookId(notebook.getId());
    jobRequest.setContent(req.getCodes());
    jobRequest = fillJobRequestContext(jobRequest, notebook, interpreter);
    long jobRequestId = zetaJobRequestRepository.addZetaJobRequest(jobRequest);

    String jobId = String.valueOf(jobRequestId);
    ExecuteCodeJob jobReady = new ExecuteCodeJob(req.getNotebookId(), jobId, req.getReqId());
    dest.sendData(WebSocketResp.get(WebSocketResp.OP.NB_CODE_JOB_READY, jobReady));
    return jobId;
  }

  public void cancel(QueueDestination dest, ExecuteCodeCancel req) {
    logger.info("Handling cancel request from user: " + dest.getUserName());
    List<Integer> stmtIds = factory
            .cleanStatements(dest.getUserName(), req.getNotebookId(), ZLivySparkSqlInterpreter.class);
    String cancelledInfo = "JobId: [" + req.getJobId() + "] cancelled" +
            (stmtIds == null ?
                    " but no statements removed, because no interpreters found" :
                    " and removed statements: [" +
                            stmtIds
                                    .stream()
                                    .map(id -> id.toString())
                                    .collect(Collectors.joining(", ")) + "]");
    ExecuteCodeCancelRsp executeSQLCancelRsp = new ExecuteCodeCancelRsp(req.getNotebookId(), req.getJobId(),
            "", ExecuteCodeCancelRsp.STATUS.CANCELLED.name());
    executeSQLCancelRsp.setInfo(cancelledInfo);
    dest.sendData(WebSocketResp.get(WebSocketResp.OP.NB_CODE_JOB_CANCELLED, executeSQLCancelRsp));
    logger.info("Cancel command executed");
  }

  public DynamicVariableHandler getDynamicVariableHandler() {
    return this.dynamicVariableHandler;
  }
}
