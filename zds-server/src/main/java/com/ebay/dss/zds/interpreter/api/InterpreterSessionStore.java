package com.ebay.dss.zds.interpreter.api;

import com.ebay.dss.zds.common.BucketReentrantLock;
import com.ebay.dss.zds.common.ThreadUtils;
import com.ebay.dss.zds.exception.InterpreterSessionNotFoundException;
import com.ebay.dss.zds.interpreter.ConfigurationManager;
import com.ebay.dss.zds.interpreter.InterpreterConfiguration;
import com.ebay.dss.zds.interpreter.InterpreterFactory;
import com.ebay.dss.zds.interpreter.InterpreterType;
import com.ebay.dss.zds.interpreter.interpreters.Interpreter;
import com.ebay.dss.zds.interpreter.interpreters.InterpreterGroup;
import com.ebay.dss.zds.runner.ZetaExecutionPool;
import com.ebay.dss.zds.service.api.ZetaExternalApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by tatian on 2021/3/30.
 */
public class InterpreterSessionStore {

  private static final Logger logger = LoggerFactory.getLogger(InterpreterSessionStore.class);

  private final ConcurrentHashMap<String, InterpreterSession> sessions;
  private final InterpreterService interpreterService;
  private final InterpreterFactory factory;
  private final ConfigurationManager configurationManager;
  private final LinkedBlockingQueue<InterpreterSession> failedSessions;
  private Thread cleanUpThread;

  public InterpreterSessionStore(InterpreterService interpreterService, ConfigurationManager configurationManager) {
    this.sessions = new ConcurrentHashMap<>();
    this.interpreterService = interpreterService;
    this.factory = interpreterService.getInterpreterManager().getFactory();
    this.configurationManager = configurationManager;
    this.failedSessions = new LinkedBlockingQueue<>();
    setCleanUpThread();
  }

  private void setCleanUpThread() {
    this.cleanUpThread = new Thread(this::swapFailedSessions);
    this.cleanUpThread.setName("InterpreterSessionStore-cleanup-thread");
    this.cleanUpThread.start();
  }

  private void swapFailedSessions() {
    while (true) {
      try {
        if (this.failedSessions.size() > 0) {
          int failed = this.failedSessions.size();
          long current = System.currentTimeMillis();
          for (int i = 0; i < failed; i ++) {
            InterpreterSession session = this.failedSessions.poll();
            if (current - session.getLastFinishTime() < 10000) {
              this.failedSessions.offer(session);
            } else {
              InterpreterSession maybeTheSame = sessions.get(session.sessionId);
              if (maybeTheSame == null) continue;
              if (maybeTheSame.equals(session)) {
                closeInterpreterSession(maybeTheSame.sessionId);
                logger.info("Swapped leak session: {}", maybeTheSame.toJson());
              } else {
                logger.info("Same session id: {} but different session, don't close it", session.sessionId);
              }
            }
          }
        }
        ThreadUtils.sleep(10000);
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }

  public InterpreterSession createInterpreterSessionAsync(String userId,
                                                          String noteId,
                                                          String intp,
                                                          Map<String, String> properties) throws Exception {
    final InterpreterSession session = new InterpreterSession(userId, noteId, intp);
    final Runnable closeHook = () -> {
      // we only remove the session and make it invisible, if this hook called, the interpreter
      // bind will be released so we only release the ref which is the session here
      markFailed(session, "Closed hook triggered by interpreter close");
      logger.info("Close hook triggered when interpreter destroy");
    };
    session.setCloseHook(closeHook);
    session.setCreator(new InterpreterCreator() {
      @Override
      public Interpreter create() throws Exception {
        interpreterService.openNote(userId, noteId, intp, properties);
        // if reach here the type must be valid
        InterpreterType.EnumType intpType = InterpreterType.fromString(intp);
        Interpreter interpreter = interpreterService.findInterpreter(noteId, userId, intpType.getInterpreterClass().getName());
        if (interpreter == null) {
          String error = "The Interpreter already created but invisible";
          logger.info(error);
          throw new IllegalStateException(error);
        } else {
          interpreter.addCloseHooks(closeHook);
        }
        return interpreter;
      }

      @Override
      public void destroy(boolean async) {
        logger.info("Destroy called for user: {}, note: {}", userId, noteId);
        interpreterService.removeNoteAsyncOrNot(userId, noteId, async);
      }
    });
    return session;
  }

  public InterpreterSession getOrCreateInterpreterSessionAsync(String userId,
                                                               String noteId,
                                                               String interpreterName,
                                                               Map<String, String> properties) throws Exception {
    String targetKey = InterpreterGroup.makeGrpKey(userId, noteId);
    InterpreterSession session = sessions.get(targetKey);
    // the interpreter could handled by the interpreter factory so we don't need to consider the
    // thread safe here, if duplicated created, the interpreter inside the session could be closed and clean in other place
    if (session == null) {
      session = createInterpreterSessionAsync(userId, noteId, interpreterName, properties);
      sessions.put(session.sessionId, session);
    }
    return session;
  }

  public InterpreterSession removeAndCreateInterpreterSessionAsync(String userId,
                                                                   String noteId,
                                                                   String interpreterName,
                                                                   Map<String, String> properties) throws Exception {
    String targetKey = InterpreterGroup.makeGrpKey(userId, noteId);
    InterpreterSession session = sessions.remove(targetKey);
    if (session != null) {
      session.close(false);
    }
    session = createInterpreterSessionAsync(userId, noteId, interpreterName, properties);
    this.sessions.put(session.sessionId, session);
    return session;
  }

  public InterpreterSession getInterpreterSession(String sessionId) throws InterpreterSessionNotFoundException {
    InterpreterSession session = sessions.get(sessionId);
    if (session == null) {
      throw new InterpreterSessionNotFoundException(sessionId);
    }
    return session;
  }

  public Optional<InterpreterSession> searchInterpreterSession(String sessionId) {
    return Optional.ofNullable(sessions.get(sessionId));
  }

  public void closeInterpreterSession(String sessionId) {
    InterpreterSession session = sessions.remove(sessionId);
    if (session != null) session.close(false);
  }

  public InterpreterSession removeSession(String sessionId) {
    InterpreterSession session = sessions.remove(sessionId);
    if (session != null) {
      session.markClosed();
    }
    return session;
  }

  public void markFailed(InterpreterSession failedSession, String comment) {
    failedSession.markError(comment);
    this.failedSessions.offer(failedSession);
    logger.info("Failed session: {} marked in queue", failedSession.toJson());
  }

}
