package com.ebay.dss.zds.interpreter.interpreters.livy;

import com.ebay.dss.zds.common.Constant;
import com.ebay.dss.zds.common.HadoopConstant;
import com.ebay.dss.zds.interpreter.InterpreterConfiguration;
import com.ebay.dss.zds.interpreter.listener.InterpreterListener;
import com.ebay.dss.zds.interpreter.listener.InterpreterListenerData;
import com.ebay.dss.zds.interpreter.monitor.modle.YarnQueue;
import com.ebay.dss.zds.interpreter.output.ConnectionProgress;
import com.ebay.dss.zds.interpreter.*;
import com.ebay.dss.zds.model.ZetaStatus;
import com.ebay.dss.zds.dao.ZetaStatementRepository;
import com.ebay.dss.zds.exception.InterpreterStoppedException;
import com.ebay.dss.zds.interpreter.ConfigurationManager.Cluster;
import com.ebay.dss.zds.interpreter.input.ExecutionContext;
import com.ebay.dss.zds.interpreter.interpreters.Interpreter;
import com.ebay.dss.zds.interpreter.interpreters.livy.LivyMessage.CreateSessionRequest;
import com.ebay.dss.zds.interpreter.interpreters.livy.LivyMessage.ExecuteRequest;
import com.ebay.dss.zds.interpreter.interpreters.livy.LivyMessage.ExecuteDumpRequest;
import com.ebay.dss.zds.interpreter.interpreters.livy.LivyMessage.MoveQueueRequest;
import com.ebay.dss.zds.interpreter.interpreters.livy.LivyMessage.SessionInfo;
import com.ebay.dss.zds.interpreter.interpreters.livy.LivyMessage.StatementInfo;
import com.ebay.dss.zds.interpreter.monitor.modle.JobReport;
import com.ebay.dss.zds.interpreter.monitor.modle.Status;
import com.ebay.dss.zds.interpreter.output.InterpreterResultBuilder;
import com.ebay.dss.zds.interpreter.output.result.JsonResult;
import com.ebay.dss.zds.model.ZetaStatement;
import com.ebay.dss.zds.packages.LivyPackagesObserver;
import com.ebay.dss.zds.serverconfig.ApplicationContextProvider;
import com.ebay.dss.zds.service.BDPHTTPService;
import com.ebay.dss.zds.message.EventTracker;
import com.ebay.dss.zds.message.event.ZetaOperationEvent.*;
import com.ebay.dss.zds.message.event.ZetaMonitorEvent.StatementStatus;
import com.ebay.dss.zds.message.event.*;
import com.ebay.dss.zds.service.DumpFileService;
import com.ebay.dss.zds.state.Recoverable;
import com.ebay.dss.zds.state.StateManager;
import com.ebay.dss.zds.state.StateSnapshot;
import com.google.gson.JsonObject;
import org.apache.commons.lang.StringUtils;
import org.apache.zeppelin.interpreter.InterpreterResult;
import org.apache.zeppelin.interpreter.InterpreterResultMessage;
import org.apache.zeppelin.interpreter.InterpreterUtils;
import org.apache.zeppelin.livy.APINotFoundException;
import org.apache.zeppelin.livy.LivyException;
import org.apache.zeppelin.livy.SessionNotFoundException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.ebay.dss.zds.interpreter.interpreters.livy.LivyClient.LIVY_CONF_HEADER;
import static com.ebay.dss.zds.interpreter.interpreters.livy.LivyClient.getResultFromStatementInfo;
import static com.ebay.dss.zds.interpreter.interpreters.livy.LivyVersion.LIVY_0_5_0;
import static com.ebay.dss.zds.message.event.Event.*;

/**
 * Created by tatian on 2018/4/25.
 */
public abstract class ZBaseLivyInterpreter extends Interpreter implements Recoverable {

    protected static final Logger LOGGER = LoggerFactory.getLogger(ZBaseLivyInterpreter.class);
    protected volatile SessionInfo sessionInfo;
    private LivyClient livyClient;
    private int sessionCreationTimeout;
    private int pullStatusInterval;
    protected boolean displayAppInfo;
    protected boolean appendSessionExpired;
    protected LivyVersion livyVersion;
    protected int maxResult = 1000;
    protected int maxDumpResult = 1000000;
    private String proxyUser;
    private int clusterId;
    private boolean clusterAccessCheck = false;
    private String connQueue;
    private List<String> unmovableTargetQueues;
    private BDPHTTPService bdphttpService;
    private ZetaStatementRepository statementRepository;
    private InterpreterManagerExecutor executor;
    // private DumpFileService dumpFileService;

    private volatile boolean isInited = false;

    // keep tracking the mapping between paragraphId and statementId, so that we can cancel the
    // statement after we execute it.
    private volatile ConcurrentHashMap<String, StmtContext> paragraphId2StmtIdMapping = new ConcurrentHashMap<>();

    private volatile String lastDag = null;

    private ConnectionStatus connectionStatus = new ConnectionStatus();

    public ZBaseLivyInterpreter(Properties property) {
        super(property);
        // todo: consider to push this LivyClient initializing to the interpreter open
        this.livyClient = new LivyClient(property, getSessionKind());
        this.proxyUser = property.getProperty(LIVY_CONF_HEADER + "username", getUserName());
        this.displayAppInfo = Boolean.parseBoolean(
                property.getProperty(LIVY_CONF_HEADER + "displayAppInfo", "true"));
        this.appendSessionExpired = Boolean.parseBoolean(
                property.getProperty(LIVY_CONF_HEADER + "appendSessionExpired", "true"));
        this.sessionCreationTimeout = Integer.parseInt(
                property.getProperty(LIVY_CONF_HEADER + "session.create_timeout", 120 + ""));
        this.pullStatusInterval = Integer.parseInt(
                property.getProperty(LIVY_CONF_HEADER + "pull_status.interval.millis", 1000 + ""));
        this.maxResult = Integer.parseInt(property.getProperty(LIVY_CONF_HEADER + "spark.sql.maxResult",
                "1000"));
        this.maxDumpResult = Integer.parseInt(property.getProperty(LIVY_CONF_HEADER + "spark.result.dump.maxResult",
                "100000000"));
        getProperties().setProperty(Constant.VIEW_LIMIT, property.getProperty(LIVY_CONF_HEADER + "spark.sql.maxResult",
                "1000"));
        this.clusterId = Integer.parseInt(property.getProperty(LIVY_CONF_HEADER + "clusterId", "-1"));
        this.clusterAccessCheck = Boolean.parseBoolean(property.getProperty(LIVY_CONF_HEADER + "cluster.access.check", "false"));
        this.connQueue = property.getProperty(LIVY_CONF_HEADER + "connectionQueue.name",
                "hdlq-data-zeta");
        this.unmovableTargetQueues = Arrays.stream(property
                .getProperty(LIVY_CONF_HEADER + "connectionQueue.target.blacklist", "sla,sle,high")
                .split(",")).map(String::toLowerCase).collect(Collectors.toList());
        this.bdphttpService = (BDPHTTPService) property.get("bdpHttpService");
        if (Objects.isNull(bdphttpService)) {
            this.bdphttpService = ApplicationContextProvider.getBean(BDPHTTPService.class);
        }
        this.statementRepository = ApplicationContextProvider.getBean(ZetaStatementRepository.class);
        this.executor = ApplicationContextProvider.getBean(InterpreterManagerExecutor.class);
        // todo: refactor dump
        // this.dumpFileService = ApplicationContextProvider.getBean(DumpFileService.class);
    }

    public int getClusterId() {
        return clusterId;
    }

    public ZBaseLivyInterpreter() {
        super(new Properties());
    }

    protected void setLivyClient(LivyClient client) {
        this.livyClient = client;
    }

    public LivyClient getLivyClient() {
        return this.livyClient;
    }

    public abstract String getSessionKind();

    public abstract String setSessionKind(String kind);

    public abstract String getCodeType();

    public abstract String setCodeType(String type);

    protected void setExecutor(InterpreterManagerExecutor executor) {
        this.executor = executor;
    }

    public boolean isOpened() {
        try {
            return !isSessionExpired() && isInited;
        } catch (LivyException ex) {
            logger.debug("Failed to check the interpreter: " + getInterpreterGroup().getGroupId() + " caused by: " + ex.toString());
            return true;
        }
    }

    private boolean isConnQueueEnabled(String queueName) {
        Boolean enabled = InterpreterConfiguration.getUseConnQueue().get(this.clusterId);
        String lowerQueueName = queueName.toLowerCase();
        /*
        boolean isSLA = lowerQueueName.contains("sla");
        boolean isSLE = lowerQueueName.contains("sle");
        boolean isHigh = lowerQueueName.contains("high");*/

        // the queue already toLowerCase() during initializing
        for (String blackQueue : unmovableTargetQueues) {
            if (blackQueue.contains(lowerQueueName)
                    || lowerQueueName.contains(blackQueue)) {
                logger.info("The queue: {} is high memory queue black list", queueName);
                return false;
            }
        }
        boolean isHighMemoryQueue = isHighMemoryQueue(queueName);
        if (isHighMemoryQueue) logger.info("The queue: {} is high memory queue", queueName);
        return enabled != null && enabled && !isHighMemoryQueue;
    }

    private boolean isHighMemoryQueue(String queueName) {
        YarnQueue queue = this.bdphttpService.getQueueInfo(Cluster.valueOfClusterId(this.clusterId), queueName);
        return queue.getNodeLabels().contains("highmemory");
    }

    private boolean isConnQueueAvailableV2() {
        // as long as the user has the access to the cluster then it can submit job to the connection queue
        return checkAccess(this.proxyUser);
    }

    @Deprecated
    private boolean isConnQueueAvailable() {
        return this.bdphttpService.getUserQueueInfo(Cluster.valueOfClusterId(this.clusterId),
                this.proxyUser).stream().filter(q -> q.equals(this.connQueue)).count() == 1;
    }

    private void moveQueue(String queue) throws LivyException {
        livyClient.moveQueue(this.sessionInfo.id, new MoveQueueRequest(queue));
    }

    private Map<String, Object> constructResult(Map<String, Object> connectionResult,
                                                boolean success,
                                                boolean usingConnectionQueue,
                                                boolean connectionQueueSuccess,
                                                String connectionQueueFailReason,
                                                long connectionQueueCost
                                                ) {
        connectionResult.put("success", success);
        connectionResult.put("usingConnectionQueue", usingConnectionQueue);
        connectionResult.put("connectionQueueSuccess", connectionQueueSuccess);
        connectionResult.put("connectionQueueFailReason", connectionQueueFailReason);
        connectionResult.put("connectionQueueCost", connectionQueueCost);
        return connectionResult;
    }

    private Map<String, Object> fillConnectionContext(Map<String, Object> connectionResult) {
        connectionResult.put("nt", getUserName());
        connectionResult.put("proxyUser", proxyUser);
        connectionResult.put("clusterId", clusterId);
        connectionResult.put("noteId", getInterpreterGroup().getNoteId());
        connectionResult.put("interpreterClass", this.getClassName());
        return connectionResult;
    }

    private Map<String, Object> tryConstructWithZetaQueue() throws InterpreterStoppedException {

        Map<String, Object> connectionResult = new HashMap<>();
        fillConnectionContext(connectionResult);

        long start = System.currentTimeMillis();
        String targetQueue = this.getProperty("zds.livy.spark.yarn.queue");
        assert(targetQueue != null);
        connectionResult.put("targetQueue", targetQueue);
        String clusterName = Cluster.valueOfClusterId(this.clusterId);
        LOGGER.info(String.format("Start to connect via zeta queue: %s, %s, %s, %s",
                this.connQueue, targetQueue, this.proxyUser, clusterName));

        if (!isConnQueueEnabled(targetQueue) || !isConnQueueAvailableV2()) {
            LOGGER.info("No available connection queue!");
            constructResult(connectionResult, false,
                    false,
                    false,
                    "No available connection queue!",
                    0L);
            return connectionResult;
        }

        boolean success = true;
        long connectionQueueStart = System.currentTimeMillis();
        try {
            this.setProperty("zds.livy.spark.yarn.queue", this.connQueue);
            initLivySession();
            LOGGER.info("In connection queue: " + this.connQueue);
            moveQueue(targetQueue);
            LOGGER.info("Move to queue: " + targetQueue);
        } catch (LivyException e) {
            constructResult(connectionResult, false,
                    true,
                    false,
                    e.toString(),
                    System.currentTimeMillis() - connectionQueueStart);
            LOGGER.warn("Fail to create session with zeta queue, please check livy interpreter " +
                    "log and livy server log", e);
            LOGGER.warn("Try to close session async");
            livyClient.closeSessionAsync(this.sessionInfo.id);
            success = false;
        } finally {
            this.setProperty("zds.livy.spark.yarn.queue", targetQueue);
        }

        long connectionQueueCost = System.currentTimeMillis() - connectionQueueStart;
        long totalCost = System.currentTimeMillis() - start;
        if (success) {
            constructResult(connectionResult, true,
                    true,
                    true,
                    "success",
                    connectionQueueCost);
            LOGGER.info(String.format("Successfully connect via zeta queue: %s, %s, %s, %s, using %s seconds",
                    this.connQueue, targetQueue, this.proxyUser, clusterName,
                    totalCost / 1000));
        } else {
            LOGGER.warn(String.format("Fail to connect via zeta queue: %s, %s, %s, %s, using %s seconds",
                    this.connQueue, targetQueue, this.proxyUser, clusterName,
                    totalCost / 1000));
        }

        return connectionResult;
    }

    private boolean checkAccess(String userName) {
        LOGGER.info("Check cluster: [] access for user: [] with proxyUser: []",
                this.clusterId, userName, this.proxyUser);
        boolean has_access = true;
        try {
            List<String> access_clusters = bdphttpService.getUserClusterInfo(userName);
            if (access_clusters == null) {
                LOGGER.warn(String.format("Failed to check user: %s's cluster access", userName));
            } else {
                has_access = access_clusters.contains(Cluster.valueOfClusterId(this.clusterId));
            }
        } catch (Exception ex) {
            LOGGER.warn(String.format("Failed to check user: %s's cluster access", userName), ex);
        }
        return has_access;
    }

    public void checkAccessOrThrow() {
        if (clusterAccessCheck) {
            String userName = getUserName();
            boolean has_access = checkAccess(userName);
            if (!has_access) {
                throw new RuntimeException(String
                        .format("User: %s has no access to cluster: %s. Please make sure you have the permission to access this cluster.\n" +
                                "Check and request for cluster access: https://bdp.corp.ebay.com/cluster/request", userName, this.clusterId));
            } else {
                LOGGER.info("User: [] has access in cluster: []", userName, this.clusterId);
            }
        }
    }

    public void construct() throws LivyException, InterpreterStoppedException {
        long start = System.currentTimeMillis();

        checkAccessOrThrow();

        String targetQueue = this.getProperty("zds.livy.spark.yarn.queue");
        assert(targetQueue != null);
        String clusterName = Cluster.valueOfClusterId(this.clusterId);
        String endpoint = livyClient.getLivyURL();
        LOGGER.info(String.format("Start to connect queue: %s, %s, %s", targetQueue, this.proxyUser, clusterName));

        EventTracker.postEvent(Event.ZetaLivyConnectionStart(getUserName(), proxyUser, clusterId, targetQueue, endpoint,
                getInterpreterGroup().getGroupId(), this.getClassName()));

        Map<String, Object> connectionResult = tryConstructWithZetaQueue();
        connectionResult.put("endpoint", endpoint);
        if (!(boolean)connectionResult.get("success")) {
            /**into step 3**/
            connectionStatus.setCurrentStep(3);
            try {
                initLivySession();
            } catch(LivyException e){
                LOGGER.warn(String.format("Fail to connect queue: %s, %s, %s, using %s seconds",
                        targetQueue, this.proxyUser, clusterName, (System.currentTimeMillis() - start) / 1000));
                String msg = e.getMessage();

                long cost = System.currentTimeMillis() - start;
                connectionResult.put("totalCost", cost);
                connectionResult.put("reason", msg);
                EventTracker.postEvent(ZetaLivyConnectionFail.fromMap(connectionResult));
                throw e;
            }
        }
        long cost = System.currentTimeMillis() - start;
        connectionResult.put("totalCost", cost);
        EventTracker.postEvent(ZetaLivyConnectionSuccess.fromMap(connectionResult));

        LOGGER.info(String.format("Successfully connect queue: %s, %s, %s, using %s seconds",
                targetQueue, this.proxyUser, clusterName, cost / 1000));
        isInited = true;
    }

    private void addPackageUploadTask() {
        try {
            Object packagesListObj = getProperties().get(InterpreterConfiguration.APPLIED_PACKAGES_LIST);
            if (packagesListObj != null) {
                LOGGER.info("Adding new package upload task for " + getInterpreterGroup().getGroupId());
                LivyPackagesObserver.requireAddResource(
                        livyClient,
                        (Set<String>)packagesListObj,
                        sessionInfo);
            } else {
                LOGGER.info("No package need to applied");
            }
        } catch (Exception ex) {
            LOGGER.error("Error when creating package upload task: " + ex.toString());
        } finally {
          getProperties().remove(InterpreterConfiguration.APPLIED_PACKAGES_LIST);
        }
    }

    @Override
    protected void afterConstruct() {
        super.afterConstruct();
        addPackageUploadTask();
    }

    @Override
    public void close() {
        if (sessionInfo != null) {
            LOGGER.info("Try to close session async");
            livyClient.closeSessionAsync(sessionInfo.id);
            livyClient.close();
            // reset sessionInfo to null so that we won't close it twice.
            sessionInfo = null;
            isInited = false;
            //sparkProgress.close();
        }
    }

    public String getProxyUser() {
        return proxyUser;
    }

    protected void initLivySession() throws LivyException,InterpreterStoppedException {
        try {
            this.livyVersion = livyClient.getLivyVersion();
        } catch (APINotFoundException e) {
            this.livyVersion = new LivyVersion("0.2.0");
        }

        if (livyVersion.olderThan(LIVY_0_5_0)) {
            setSessionKind("spark");
            setCodeType(null);
        }
        this.sessionInfo = createSession(getProxyUser(), getSessionKind());
        if (displayAppInfo) {
            if (sessionInfo.appId == null) {
                // livy 0.2 don't return appId and sparkUiUrl in response so that we need to get it
                // explicitly by ourselves.
                sessionInfo.appId = extractAppId();
            }

            if (sessionInfo.appInfo == null ||
                    StringUtils.isEmpty(sessionInfo.appInfo.get("sparkUiUrl"))) {
                sessionInfo.webUIAddress = extractWebUIAddress();
            } else {
                sessionInfo.webUIAddress = sessionInfo.appInfo.get("sparkUiUrl");
            }
            LOGGER.info("Create livy session successfully with sessionId: {}, sessionKind: {}, appId: {}, webUI: {}",
                    sessionInfo.id, getSessionKind(), sessionInfo.appId, sessionInfo.webUIAddress);
        } else {
            LOGGER.info("Create livy session successfully with sessionId: {}", this.sessionInfo.id);
        }
        // check livy version
        LOGGER.info("Use livy " + livyVersion);
        isInited = true;
    }

    public void setDisplayAppInfo(boolean displayAppInfo) {
        this.displayAppInfo = displayAppInfo;
    }

    protected abstract String extractAppId() throws LivyException;

    protected abstract String extractWebUIAddress() throws LivyException;

    public SessionInfo getSessionInfo() {
        return sessionInfo;
    }

    boolean isCancelSupported() {
        return livyVersion.isCancelSupported();
    }

    @Override
    public void cancel(ExecutionContext context) {
        if (isCancelSupported()) {
            String paraId = context.getParagraphId().toString();
            StmtContext stmtContext = getParagraphId2StmtIdMapping().get(paraId);
            try {
                if (stmtContext != null && stmtContext.statementId!=null) {
                    livyClient.cancelStatement(getSessionInfo().id, stmtContext.statementId);
                }
            } catch (LivyException e) {
                LOGGER.error("Fail to cancel statement " + stmtContext.statementId + " for paragraph " + paraId, e);
            } finally {
                getParagraphId2StmtIdMapping().remove(paraId);
            }
        } else {
            LOGGER.warn("cancel is not supported for this version of livy: " + livyVersion);
        }
    }

    ConcurrentHashMap<String, StmtContext> getParagraphId2StmtIdMapping() {
        return paragraphId2StmtIdMapping;
    }

    @Override
    public List<Integer> cancelAll() {
        ArrayList<String> pids = new ArrayList<>();
        ArrayList<Integer> stmtids = new ArrayList<>();
        /**In case new objects are putted into the map
         * only cancel the statement at the point when the method is called
         * will not be safe when sharing the interpreter among different user
         * **/
        try {
            paragraphId2StmtIdMapping
                    .entrySet().
                    stream()
                    .forEach(kv -> {
                        pids.add(kv.getKey());
                        stmtids.add(kv.getValue().statementId);
                    });

            stmtids.stream()
                    .filter(stmtId -> stmtId != null)
                    .map(stmtId -> {
                        try {
                            livyClient.cancelStatement(sessionInfo.id, stmtId);
                        } catch (LivyException e) {
                            LOGGER.error("Fail to cancel statement " + stmtId, e);
                        }
                        return stmtId;
                    }).forEach(stmtId -> makeSureCanceled(sessionInfo.id, stmtId));
            DateTime endDt = new DateTime();
            pids.forEach(paragraphId -> {
                Long zetaStatementKey = paragraphId2StmtIdMapping.get(paragraphId).zetaStatementKey;
                try {
                    if (zetaStatementKey != null) {
                        JsonResult result = new JsonResult();
                        result.setProperty("dag",lastDag);
                        statementRepository.updateUnfinishedZetaStatementStateById(zetaStatementKey, ZetaStatus.CANCELED.getStatusCode(), result.get(), 0, new Timestamp(endDt.getMillis()));
                        logger.info("Canceled: Notebook[id=" + getInterpreterGroup().getNoteId() + "]");
                    }
                } catch (Exception ex) {
                    logger.error("Exception happened during update state: " + ex.getMessage());
                }
                paragraphId2StmtIdMapping.remove(paragraphId);
            });

        }catch (Exception ex){
            ex.printStackTrace();
            logger.error("Exception when cancelling: "+ex.toString());
        }
        return stmtids;
    }

    public Boolean makeSureCanceled(Integer sessionId, Integer statementId) {
        int count = Integer.parseInt(
                getProperties().getProperty(LIVY_CONF_HEADER + "cancel.times", 5 + ""));
        try {
            while (livyClient.isAlive(sessionId, statementId)) {
                if (count == 0) {
                    logger.info("Wait too many times for cancelling, exit cancelling, assume it canceled");
                    break;
                }
                StatementInfo si=livyClient.getStatementInfo(sessionId, statementId);
                if (si.isCancelling()) {
                    logger.info("Cancelling Statement: {} of Session: {}", statementId, sessionId);
                } else if(si.isCancelled()){
                    break;
                } else if(si.isRunning()){
                    logger.info("Do cancelling Statement: {} of Session: {}", statementId, sessionId);
                    livyClient.cancelStatement(sessionId, statementId);
                }
                Thread.sleep(pullStatusInterval);
                count--;
            }
            return true;
        } catch (Exception e) {
            logger.info("Statement: {} of Session: {} should be canceled ", statementId, sessionId);
            return true;
        }
    }

    public void checkPause()throws InterpreterStoppedException{
        if (getPauseFlag()) {
            //listener.handleCancelling(listenerData);
            LOGGER.info("Interpreter is stopped by user: {}", getUserName());
            throw new InterpreterStoppedException("Interpreter is stopped by user: " + getUserName());
        }
    }


    @Override
    public FormType getFormType() {
        return FormType.NATIVE;
    }

    @Override
    public int getProgress(ExecutionContext context) {
        return 0;
    }

    public double getStatementProgress(StatementInfo stmtInfo) {
        return 0;
    }

    SessionInfo createSession(String user, String kind)
            throws LivyException,InterpreterStoppedException {
        try {
            Map<String, String> conf = new HashMap<>();
            for (Map.Entry<Object, Object> entry : getProperties().entrySet()) {
                if (entry.getKey().toString().startsWith(LIVY_CONF_HEADER + "spark.") &&
                        !entry.getValue().toString().isEmpty())
                    conf.put(entry.getKey().toString().substring(LIVY_CONF_HEADER.length()), entry.getValue().toString());
            }

            /** add the zeta context here **/
            conf.put("spark.zeta.realUser", getUserName());
            conf.put("spark.zeta.notebookId", getInterpreterGroup().getNoteId());

            CreateSessionRequest request = new CreateSessionRequest(kind,
                    user == null || user.equals("anonymous") ? null : user, conf);
            this.sessionInfo = livyClient.createSession(request);

            /**update connection status**/
            if (!isInited) {
                updateAndPostProgress();
            }

            long start = System.currentTimeMillis();
            // pull the session status until it is idle or timeout
            while (!sessionInfo.isReady()) {
                checkPause();
                if ((System.currentTimeMillis() - start) / 1000 > sessionCreationTimeout) {
                    String msg = "The creation of session " + sessionInfo.id + " is timeout within "
                            + sessionCreationTimeout + " seconds, appId: " + sessionInfo.appId
                            + ", log: " + sessionInfo.log;
                    throw new LivyException(msg);
                }
                Thread.sleep(pullStatusInterval);
                this.sessionInfo = livyClient.getSessionInfo(sessionInfo.id);
                LOGGER.info("Session {} is in state {}, appId {}", sessionInfo.id, sessionInfo.state,
                        sessionInfo.appId);

                /**update connection status**/
                if (!isInited) {
                    updateAndPostProgress();
                }
                if (sessionInfo.isFinished()) {
                    String logs = livyClient.getSessionLog(sessionInfo.id).asString();
                    // LOGGER.info(logs);
                    String msg = String.format("Session %s is in state: %s, appId: %s, queue: %s, reason: %s",
                            sessionInfo.id, sessionInfo.state, sessionInfo.appId, conf.get("spark.yarn.queue"),
                            // LivyResultParser.extractErrorCause(logs)
                            logs);
                    throw new LivyException(msg);
                }
            }
            return sessionInfo;
        } catch (Exception e) {
            if(e instanceof InterpreterStoppedException){
                throw (InterpreterStoppedException)e;
            } else if(e instanceof LivyException) {
                throw (LivyException)e;
            } else {
                LOGGER.error("Error when creating livy session for user " + user, e);
                throw new LivyException(e);
            }
        }
    }

    protected void updateSessionInfo(SessionInfo latest) {
      String latestUrl = latest.appInfo.get("sparkUiUrl");
      if (StringUtils.isNotEmpty(latestUrl)) {
        sessionInfo.webUIAddress = latestUrl;
      }
    }

    protected InterpreterResult interpret(String code,
                                          String paragraphId) {
        return interpret(code, getCodeType(), paragraphId, displayAppInfo, appendSessionExpired);
    }

    protected InterpreterResult interpret(String code,
                                          String paragraphId,
                                          InterpreterListener listener) {
        return interpret(code, getCodeType(), paragraphId, displayAppInfo, appendSessionExpired, listener);
    }

    public InterpreterResult interpret(String code,
                                          String codeType,
                                          String paragraphId,
                                          boolean displayAppInfo,
                                          boolean appendSessionExpired,
                                          InterpreterListener listener) {
        ExecuteRequest executeRequest = new ExecuteRequest(code, codeType);
        return interpret(executeRequest, paragraphId, displayAppInfo, appendSessionExpired, listener);
    }

    public InterpreterResult interpret(String code,
                                          String codeType,
                                          String paragraphId,
                                          boolean displayAppInfo,
                                          boolean appendSessionExpired) {
        ExecuteRequest executeRequest = new ExecuteRequest(code, codeType);
        return interpret(executeRequest, paragraphId, displayAppInfo, appendSessionExpired);
    }

    public InterpreterResult interpret(ExecuteRequest executeRequest,
                                       String paragraphId,
                                       boolean displayAppInfo,
                                       boolean appendSessionExpired,
                                       InterpreterListener listener) {
        StatementInfo stmtInfo = null;
        boolean sessionExpired = false;

        lastDag = null;

        InterpreterListenerData listenerData = new InterpreterListenerData();
        try {
            synchronized (this) {
                if (isSessionExpired()) {
                    sessionExpired = true;
                    initLivySession();
                }
            }

            logger.info("Clearing memory store...");
            clearStore();
            logger.info("Cleared");

            /**Before Statement Submit**/
            listenerData.setSessionId(sessionInfo.id);
            listenerData.setSparkJobUrl(sessionInfo.webUIAddress);
            listener.beforeStatementSubmit(listenerData);
            /**Before Statement Submit**/
            try {
                stmtInfo = livyClient.execute(sessionInfo.id, executeRequest);
            } catch (SessionNotFoundException e) {
                LOGGER.warn("Livy session {} is expired, new session will be created.", sessionInfo.id);
                sessionExpired = true;
                // we don't want to create multiple sessions because it is possible to have multiple thread
                // to call this method, like LivySparkSQLInterpreter which use ParallelScheduler. So we need
                // to check session status again in this sync block
                synchronized (this) {
                    if (isSessionExpired()) {
                        initLivySession();
                    }
                }
                stmtInfo = livyClient.execute(sessionInfo.id, executeRequest);
            }

            int stmtId = stmtInfo.id;

            /**After Statement Submit**/
            listenerData.setStatementId(stmtId);
            checkPause();
            Long zetaStatementKey=listener.afterStatementSubmit(listenerData);
            /**After Statement Submit**/

            if (paragraphId != null) {
                paragraphId2StmtIdMapping.put(paragraphId, new StmtContext(stmtId,zetaStatementKey));
            }
            // pull the statement status
            double progress = 0.0;
            /**Update progress**/
            listener.statementProgress(listenerData, stmtInfo.getJobReport().setProgress(progress));
            //postLivyStatementMonitorEvent(StatementStatus.START, stmtInfo, executeRequest);
            /**Update progress**/
            while (!stmtInfo.isAvailable()) {

                checkPause();

                try {
                    Thread.sleep(pullStatusInterval);
                } catch (InterruptedException e) {
                    LOGGER.error("InterruptedException when pulling statement status.", e);
                    throw new LivyException(e);
                }

                stmtInfo = livyClient.getStatementInfo(sessionInfo.id, stmtId);

                if (stmtInfo.id == null) stmtInfo.id = stmtId;
                /**Update progress**/
                logger.debug("SparkProgress:  application id = [{}] statement id = [{}] progress = [{}]", sessionInfo.appId, stmtInfo.id, stmtInfo.progress);
                listener.statementProgress(listenerData, stmtInfo.getJobReport());
                // postLivyStatementMonitorEvent(StatementStatus.RUNNING, stmtInfo, executeRequest);
                /**Update progress**/
                String op_dag = ((JobReport)stmtInfo.getJobReport()).dag;
                if (StringUtils.isNotEmpty(op_dag)) lastDag = op_dag;
            }
            /**Update progress**/
            /**Whether succeeded or not we assume that the process is ended**/
            progress = 1.0;
            listener.statementProgress(listenerData, stmtInfo.getJobReport().setProgress(progress).setStatus(Status.StatusEnum.WAITING));
            // postLivyStatementMonitorEvent(StatementStatus.END, stmtInfo, executeRequest);
            /**Update progress**/

            /**After statement finish**/
            JsonResult jsonResult;
            InterpreterResult intpResult;
            InterpreterResult finalInterpreterResult;

            /** dump request**/
            if (executeRequest instanceof ExecuteDumpRequest) {

                if (appendSessionExpired) {
                    intpResult = appendSessionExpire(getResultFromStatementInfo(sessionInfo, stmtInfo, displayAppInfo),
                            sessionExpired);
                } else {
                    intpResult = getResultFromStatementInfo(sessionInfo, stmtInfo, displayAppInfo);
                }

                InterpreterResultMessage message = intpResult.getResultMessage(InterpreterResult.Type.TABLE);
                message.setType(InterpreterResult.Type.TEXT);

                getStore().store(stmtInfo.id, intpResult);
                logger.info("Dump result stored for: " + getInterpreterGroup().getGroupId());
                logger.info("Result limit: " + maxResult);

                if (listener.getListenerType().equals(InterpreterListener.ListenerType.SQL)) {
                    finalInterpreterResult = ZLivyCommonUtil.limitCSVResult(intpResult, maxResult);
                    jsonResult = new JsonResult(finalInterpreterResult);
                } else {
                    jsonResult = new JsonResult(intpResult);
                    finalInterpreterResult = intpResult;
                }

            } else {

                /** not dump**/
                if (appendSessionExpired) {
                    intpResult = appendSessionExpire(getResultFromStatementInfo(sessionInfo, stmtInfo, displayAppInfo),
                            sessionExpired);
                } else {
                    intpResult = getResultFromStatementInfo(sessionInfo, stmtInfo, displayAppInfo);
                }

                if (listener.getListenerType().equals(InterpreterListener.ListenerType.SQL)) {
                    intpResult = resolveSQLOutput(intpResult, maxResult);
                }
                jsonResult = new JsonResult(intpResult);
                finalInterpreterResult = intpResult;
            }

            jsonResult.setProperty("dag", lastDag);
            listener.afterStatementFinish(listenerData, jsonResult);
            /**After statement finish**/

            if (LivyClient.isDagUnfinished(lastDag)) {
                doPullDagAsync(stmtId, listener.getCurrentStatementKey(),5);
            }

            return finalInterpreterResult;
        } catch (InterpreterStoppedException e) {
            // in this case, the dag is either not exists or already stored into db by another method: cancelAll
            return new InterpreterResult(InterpreterResult.Code.SUCCESS, "Interpreter is stopped");
        } catch (Exception e) {
            LOGGER.error("Fail to interpret:" + executeRequest.code, e);
            DateTime endDt = new DateTime();
            JsonResult result = new JsonResult();
            result.setProperty("dag",lastDag);
            Long zetaStatementKey = paragraphId2StmtIdMapping.get(paragraphId).zetaStatementKey;
            statementRepository
                    .updateUnfinishedZetaStatementStateById(zetaStatementKey, ZetaStatus.FAIL.getStatusCode(),
                            result.get(), 0, new Timestamp(endDt.getMillis()));
            if (e instanceof LivyException) {
                return new InterpreterResult(InterpreterResult.Code.ERROR,
                        InterpreterUtils.getMostRelevantMessage(e));
            } else if (e instanceof NullPointerException) {
                String msg = "Unable to get the result of this statement, maybe the the statement was failed\n" +
                        "Please check the spark url for details: " + sessionInfo.webUIAddress;
                return new InterpreterResult(InterpreterResult.Code.ERROR, msg);
            } else return new InterpreterResult(InterpreterResult.Code.ERROR, e.toString());
        } finally {
            if (paragraphId != null) {
                paragraphId2StmtIdMapping.remove(paragraphId);
            }
            // postLivyStatementMonitorEvent(StatementStatus.END, stmtInfo, executeRequest);
        }
    }

    protected InterpreterResult interpret(ExecuteRequest executeRequest,
                                          String paragraphId,
                                          boolean displayAppInfo,
                                          boolean appendSessionExpired) {

        StatementInfo stmtInfo = null;
        boolean sessionExpired = false;
        try {
            try {
                stmtInfo = livyClient.execute(sessionInfo.id, executeRequest);
            } catch (SessionNotFoundException e) {
                LOGGER.warn("Livy session {} is expired, new session will be created.", sessionInfo.id);
                sessionExpired = true;
                // we don't want to create multiple sessions because it is possible to have multiple thread
                // to call this method, like LivySparkSQLInterpreter which use ParallelScheduler. So we need
                // to check session status again in this sync block
                synchronized (this) {
                    if (isSessionExpired()) {
                        initLivySession();
                    }
                }
                stmtInfo = livyClient.execute(sessionInfo.id, executeRequest);
            }
            if (paragraphId != null) {
                paragraphId2StmtIdMapping.put(paragraphId, new StmtContext(stmtInfo.id,null));
            }
            // pull the statement status
            while (!stmtInfo.isAvailable()) {
                if (getPauseFlag()) {
                    LOGGER.error("Interpreter is stopped by user: {}", getUserName());
                    throw new InterpreterStoppedException("Interpreter is stopped by user: " + getUserName());
                }
                try {
                    Thread.sleep(pullStatusInterval);
                } catch (InterruptedException e) {
                    LOGGER.error("InterruptedException when pulling statement status.", e);
                    throw new LivyException(e);
                }
                stmtInfo = livyClient.getStatementInfo(sessionInfo.id, stmtInfo.id);
            }
            if (appendSessionExpired) {
                return appendSessionExpire(getResultFromStatementInfo(sessionInfo, stmtInfo, displayAppInfo),
                        sessionExpired);
            } else {
                return getResultFromStatementInfo(sessionInfo, stmtInfo, displayAppInfo);
            }
        } catch (InterpreterStoppedException e) {
            // in this case, the dag is either not exists or store into db by another method: cancelAll
            return new InterpreterResult(InterpreterResult.Code.SUCCESS, "Interpreter is stopped");
        } catch (Exception e) {
            if (e instanceof LivyException) {
                return new InterpreterResult(InterpreterResult.Code.ERROR,
                        InterpreterUtils.getMostRelevantMessage(e));
            } else if (e instanceof NullPointerException) {
                String msg = "Unable to get the result of this statement, " +
                        "maybe the the statement was failed\n" +
                        "Please check the spark url for details: " +
                        (Objects.isNull(sessionInfo) ? null: sessionInfo.webUIAddress);
                return new InterpreterResult(InterpreterResult.Code.ERROR, msg);
            } else {
                return new InterpreterResult(InterpreterResult.Code.ERROR, e.toString());
            }
        } finally {
            if (paragraphId != null) {
                paragraphId2StmtIdMapping.remove(paragraphId);
            }
        }
    }

    boolean isSessionExpired() throws LivyException {
        try {
            if (getSessionInfo() == null) {
                return true;
            }
            livyClient.getSessionInfo(getSessionInfo().id);
            // todo: already fixed the status inconsistency in Livy side, not sure if we need to recover in here too
            // it's not a good idea to update every time
//            SessionInfo latest = livyClient.getSessionInfo(getSessionInfo().id);
//            updateSessionInfo(latest);
            return false;
        } catch (SessionNotFoundException e) {
            return true;
        }
    }

    InterpreterResult appendSessionExpire(InterpreterResult result, boolean sessionExpired) {
        if (sessionExpired) {
            String expireHtml = "<font color=\"red\">Previous livy session is expired, new livy session is created. " +
                            "Paragraphs that depend on this paragraph need to be re-executed!</font>";
            return new InterpreterResultBuilder()
                    .code(result.code())
                    .html(expireHtml)
                    .messages(result.message())
                    .build();
        } else {
            return result;
        }
    }

    @Override
    public void doGc(){
        /*
        ClientConnectionManager cm=livyClient.getConnectionManager();
        if(cm!=null) {
            livyClient.getConnectionManager().closeIdleConnections(60, TimeUnit.SECONDS);
        }*/
    }
    protected abstract InterpreterResult resolveSQLOutput(InterpreterResult result, int maxResult);

    public static class StmtContext {
        public Integer statementId;
        public Long zetaStatementKey;

        public StmtContext(Integer statementId,Long zetaStatementKey){
            this.statementId=statementId;
            this.zetaStatementKey=zetaStatementKey;
        }
    }

    void doPullDag(int statementId, long dbStatementKey, int maxTry, ZetaStatementRepository zetaStatementRepository) {
        int tried = 0;
        while (tried < maxTry) {

            try {
                String dag = livyClient.getStatementDag(getSessionInfo().id, statementId);
                if (LivyClient.isDagFinished(dag)) {
                    ZetaStatement statement = zetaStatementRepository.getZetaStatement(dbStatementKey);
                    JsonObject result = JsonResult
                            .getParser()
                            .parse(statement.getResult())
                            .getAsJsonObject();
                    result.addProperty("dag", dag);

                    int ret = zetaStatementRepository.updateZetaStatementResultById(dbStatementKey, result.toString());
                    LOGGER.info("DAG updated for session [{}]'s statement [{}] ret [{}]",
                            getSessionInfo().id, statement, ret);
                    return;

                }
            } catch (LivyException ex) {
                LOGGER.error("LivyException when pulling dag. this is the {}'s try", ex, tried + 1);
            } catch (Exception other) {
                LOGGER.error("Exception when pulling dag. exit", other);
                break;
            }

            try {
                Thread.sleep(pullStatusInterval);
            } catch (InterruptedException e) {
                LOGGER.error("InterruptedException when pulling dag. exit", e);
                break;
            }

            tried++;
        }
        LOGGER.warn("tried {} (max: {}) but still got unfinished dag for" +
                        " session [{}], statement [{}] statementKey [{}], exit...", tried, maxTry,
                getSessionInfo().id, statementId, dbStatementKey);
        return;
    }

    void doPullDagAsync(int statementId, long dbStatementKey, int maxTry) {
        // this is a shared executor
        executor.submit(() -> doPullDag(statementId, dbStatementKey, maxTry, statementRepository));
    }

    private void updateAndPostProgress() {
        connectionStatus.updateStatus(this.sessionInfo);
        ConnectionProgress connectionProgress = new ConnectionProgress(getUserName(),
                getInterpreterGroup().getNoteId(),
                getClassName(),
                connectionStatus.getCurrentProgress(),
                connectionStatus.getCurrentMessage());
        EventTracker.postEvent(ZetaWebSocketContentEvent(getUserName(), connectionProgress.toWebSocketResp()));
    }

    private boolean postLivyStatementMonitorEvent(StatementStatus status, StatementInfo stmtInfo, ExecuteRequest executeRequest) {

        // this primary identifier can't be null
        if (stmtInfo == null || stmtInfo.id == null) return false;

        ZetaMonitorEvent.LivyStatementMonitorEvent monitorEvent;

        if (executeRequest instanceof ExecuteDumpRequest) {
            monitorEvent = LivyDumpStatementMonitorEvent(status,
                    stmtInfo.id,
                    getUserName(),
                    getProxyUser(),
                    getInterpreterGroup().getNoteId(),
                    getClassName(),
                    clusterId,
                    (JobReport) stmtInfo.getJobReport());
        } else {
            monitorEvent = LivyStatementMonitorEvent(status,
                    stmtInfo.id,
                    getUserName(),
                    getProxyUser(),
                    getInterpreterGroup().getNoteId(),
                    getClassName(),
                    clusterId,
                    (JobReport) stmtInfo.getJobReport());
        }

        monitorEvent.addProperty("executeRequestClass", executeRequest.getClass().getSimpleName());
        return EventTracker.postEvent(monitorEvent);
    }

    @Override
    public void propsDescribe(Map<String, Object> describe) {
        String currentQueue = this.getProperty("zds.livy.spark.yarn.queue");
        if (currentQueue != null) {
            describe.put(HadoopConstant.QUEUE, currentQueue);
        }
        if (this.sessionInfo != null) {
            String appId = sessionInfo.appId;
            if (appId != null) {
                describe.put(HadoopConstant.APP_ID, appId);
            }
        }
        describe.put(HadoopConstant.REAL_USER, getUserName());
        describe.put(HadoopConstant.PROXY_USER, proxyUser);
    }

    public static class ConnectionStatus {

        private int currentStep = 0;
        private double connectionQueueProgress = 0.0;
        private double targetQueueProgress = 0.0;
        private String currentMessage = "Connection request is submitted";
        private SessionInfo lastSessionInfo;

        public void updateStatus(SessionInfo sessionInfo) {
            updateProgress(updateCurrentStep(sessionInfo));
        }

        public int updateCurrentStep(SessionInfo sessionInfo) {

            if (lastSessionInfo == null) {
                currentStep = 1;
            } else {
                if (sessionInfo.appId != null) {
                    if (currentStep < 3) {
                        currentStep = 2;
                    } else {
                        currentStep = 4;
                    }
                }
            }
            lastSessionInfo = sessionInfo;
            return currentStep;
        }

        private void updateProgress(int step) {
            switch (step) {
                case 1:
                    connectionQueueProgress = 0.1;
                    currentMessage = "Session is in connection queue, state: Submitted";
                    break;
                case 2:
                    connectionQueueProgress = 0.4;
                    currentMessage = "Session is in connection queue, state: Starting";
                    break;
                case 3:
                    targetQueueProgress = 0.1;
                    currentMessage = "Session is in target queue, state: Submitted";
                    break;
                case 4:
                    targetQueueProgress = 0.4;
                    currentMessage = "Session is in target queue, state: Starting";
                    break;
                default:
                    logger.error("Invalid step: " + step);
                    break;

            }
        }

        public int getCurrentStep() {
            return currentStep;
        }

        protected void setCurrentStep(int step) {
            this.currentStep = step;
            updateProgress(currentStep);
        }

        public double getCurrentProgress() {
            return connectionQueueProgress + targetQueueProgress;
        }

        public String getCurrentMessage() {
            return currentMessage;
        }
    }

    @Override
    public StateSnapshot createSnapshot() {
        // todo: implement
        return StateSnapshot.EMPTY;
    }

    @Override
    public boolean doRecover(StateSnapshot stateSnapshot) {
        LOGGER.info("Start to recover from previous status...");
        // todo: implement
        LOGGER.info("No need to recover status");
        return false;
    }

    @Override
    public String recoverKey() {
        return "";
    }
}
