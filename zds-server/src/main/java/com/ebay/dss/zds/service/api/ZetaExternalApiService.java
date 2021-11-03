package com.ebay.dss.zds.service.api;

import com.ebay.dss.zds.exception.InterpreterSessionNotFoundException;
import com.ebay.dss.zds.interpreter.ConfigurationManager;
import com.ebay.dss.zds.interpreter.InterpreterConfiguration;
import com.ebay.dss.zds.interpreter.InterpreterType;
import com.ebay.dss.zds.interpreter.api.InterpreterService;
import com.ebay.dss.zds.interpreter.api.InterpreterSession;
import com.ebay.dss.zds.interpreter.api.InterpreterSessionStore;
import com.ebay.dss.zds.interpreter.api.InterpreterStatement;
import com.ebay.dss.zds.interpreter.api.dto.ExecuteCodeRequest;
import com.ebay.dss.zds.interpreter.api.dto.InterpreterSessionRequest;
import com.ebay.dss.zds.interpreter.interpreters.InterpreterGroup;
import com.ebay.dss.zds.model.ZetaResponse;
import com.ebay.dss.zds.rest.annotation.AuthenticationNT;
import com.ebay.dss.zds.runner.ZetaExecutionPool;
import org.codehaus.plexus.util.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by tatian on 2021/3/30.
 */
@Service
public class ZetaExternalApiService {

  private static final Logger logger = LoggerFactory.getLogger(ZetaExternalApiService.class);

  private final InterpreterService interpreterService;
  private final InterpreterSessionStore sessionStore;
  private final ZetaExecutionPool zetaExecutionPool;


  @Autowired
  public ZetaExternalApiService(InterpreterService interpreterService,
                                ConfigurationManager configurationManager,
                                ZetaExecutionPool zetaExecutionPool) {
    this.interpreterService = interpreterService;
    this.sessionStore = new InterpreterSessionStore(interpreterService, configurationManager);
    this.zetaExecutionPool = zetaExecutionPool;
  }

  public InterpreterSession tryOpenInterpreterSessionAsync(InterpreterSessionRequest request) throws Exception {
    InterpreterSession interpreterSession = sessionStore.getOrCreateInterpreterSessionAsync(request.getUserId(),
            request.getNoteId(),
            request.getInterpreter(),
            request.getProps());
    String sessionJson = interpreterSession.toJson();
    if (interpreterSession.isHealthy()) {
      if (request.reuseSession()) {
        logger.info("Reusing existing session: {}", sessionJson);
        return interpreterSession;
      } else {
        logger.info("Create for new session, old is: {}", sessionJson);
        interpreterSession = sessionStore.removeAndCreateInterpreterSessionAsync(request.getUserId(),
                request.getNoteId(),
                request.getInterpreter(),
                request.getProps());
        logger.info("Create for new session done, old is: {}", sessionJson);
      }
    } else if (interpreterSession.isFinished()) {
      logger.info("Create for new session, old not healthy is: {}", sessionJson);
      interpreterSession = sessionStore.removeAndCreateInterpreterSessionAsync(request.getUserId(),
              request.getNoteId(),
              request.getInterpreter(),
              request.getProps());
      logger.info("Create for new session done, old is: {}", sessionJson);
    }
    final AtomicReference<InterpreterSession> sessRef = new AtomicReference<>();
    sessRef.set(interpreterSession);
    String tenantName = InterpreterType.fromString(request.getInterpreter()).getName();
    executeInPoolOrCurrentThread(tenantName, () -> {
      try {
        logger.info("Start to initializing: " + sessionJson);
        sessRef.get().initialize();
        logger.info("Initialized: " + sessRef.get().toJson());
      } catch (Exception ex) {
        // Why we don't close it immediately is because the client might lost the information that why this session lost
        String fullEx = ExceptionUtils.getFullStackTrace(ex);
        sessionStore.markFailed(sessRef.get(), fullEx);
        logger.error("Error when initializing Interpreter session with request: {}, ex: {}",
                sessionJson, fullEx);
      }
    });
    logger.info("Interpreter session registered: {}", sessionJson);
    return interpreterSession;
  }

  public InterpreterSession getInterpreterSession(String id) throws InterpreterSessionNotFoundException {
    return sessionStore.getInterpreterSession(id);
  }

  public void closeInterpreterSession(String id) throws InterpreterSessionNotFoundException {
    sessionStore.closeInterpreterSession(id);
  }

  public InterpreterStatement tryExecuteStatementAsync(ExecuteCodeRequest request) throws Exception {
    InterpreterSession session = sessionStore.getInterpreterSession(request.sessionId);
    if (!session.isUsable())
      throw new IllegalStateException("The interpreter session is not usable: " + session.toJson());
    InterpreterStatement statement = session.prepareStatement(request.code);
    String tenantName = InterpreterType.fromString(session.getInterpreterName()).getName();
    executeInPoolOrCurrentThread(tenantName, statement::execute);
    return statement;
  }

  public InterpreterStatement getStatement(String sessionId, long statementId) throws Exception {
    InterpreterSession session = sessionStore.getInterpreterSession(sessionId);
    return session.getStatement(statementId)
            .orElseThrow(() -> new RuntimeException(String.format("The statement: %s is not found in session: %s", statementId, sessionId)));
  }

  public boolean cancelStatement(String sessionId, long statementId) throws Exception {
    Optional<InterpreterSession> session = sessionStore.searchInterpreterSession(sessionId);
    return session.map(self -> self.getStatement(statementId).map(stmt -> {
      stmt.cancel();
      return true;
    }).orElse(false))
            .orElse(false);
  }

  private void executeInPoolOrCurrentThread(String tenantName, Runnable runnable) {
    if (zetaExecutionPool.isEnabled()) {
      zetaExecutionPool.submit(tenantName, runnable);
    } else {
      logger.warn("The ZetaExecutionPool is not enabled for: {}, execute in current thread: {}", tenantName, Thread.currentThread().getName());
      runnable.run();
    }
  }
}
