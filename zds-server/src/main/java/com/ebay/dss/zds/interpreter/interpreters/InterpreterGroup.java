package com.ebay.dss.zds.interpreter.interpreters;

import com.ebay.dss.zds.common.Constant;
import com.ebay.dss.zds.exception.ErrorCode;
import com.ebay.dss.zds.exception.InterpreterException;
import com.ebay.dss.zds.exception.InterpreterStoppedException;
import com.ebay.dss.zds.exception.LifeCycleException;
import com.ebay.dss.zds.interpreter.InterpreterFactory;
import com.ebay.dss.zds.interpreter.annotation.InterpreterLifeCycle;
import com.ebay.dss.zds.interpreter.annotation.ShouldBeQuick;
import com.ebay.dss.zds.interpreter.annotation.TransactionGuarantee;
import com.ebay.dss.zds.interpreter.lifecycle.HealthReport;
import com.ebay.dss.zds.interpreter.lifecycle.LifeCycleManager;
import com.ebay.dss.zds.interpreter.lifecycle.LifeCycleModel;
import com.ebay.dss.zds.message.EventTracker;
import com.ebay.dss.zds.state.Recoverable;
import com.ebay.dss.zds.state.StateManager;
import com.ebay.dss.zds.state.annotation.StateSourcePrefer;
import com.ebay.dss.zds.state.source.redis.RedisStateSource;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.codehaus.plexus.util.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.ebay.dss.zds.interpreter.interpreters.InterpreterGroup.StatusCode.*;
import static com.ebay.dss.zds.message.event.Event.ZetaStatePersistEvent;

/**
 * Created by tatian on 2018/4/23.
 */
@StateSourcePrefer(prefer = RedisStateSource.class)
public class InterpreterGroup {

  private static final Logger logger = LoggerFactory.getLogger(InterpreterGroup.class);
  private String groupId;
  private String userName;
  private String noteId;

  private InterpreterFactory _factory;
  private LifeCycleModel lifeCycle;

  private Map<InterpreterClassInfo, Interpreter> interpreterMap = new ConcurrentHashMap<>();
  private volatile AtomicInteger status = new AtomicInteger(CREATED);

  private volatile boolean shutDownAsync = true;

  public InterpreterGroup(String groupId) {
    this.groupId = groupId;
  }

  public InterpreterGroup(String groupId, LifeCycleModel lifeCycle) {
    this.groupId = groupId;
    this.lifeCycle = lifeCycle;
  }

  public synchronized Interpreter getInterpreter(String className) {
    logger.info("Looking for interpreter: {}...", className);
    Optional<InterpreterClassInfo> classInfo = getClassInfo(className);
    Interpreter interpreter = null;
    if (classInfo.isPresent()) {
      interpreter = interpreterMap.get(classInfo.get());
      logger.info("Found one: {}", className);
    } else {
      logger.info("Found nothing about: {}", classInfo);
    }
    return interpreter;
  }

  public InterpreterGroup setLifeCycle(LifeCycleModel lifeCycle) {
    this.lifeCycle = lifeCycle;
    return this;
  }

  public void keepAlive() {
    if (lifeCycle != null) {
      lifeCycle.keepAlive();
    }
  }

  @NotNull
  public HealthReport getHealthReport() {
    if (lifeCycle == null) return new HealthReport(false);
    HealthReport report = lifeCycle.getHealthReport();
    if (report == null) {
      logger.warn("Got an empty health report for group: {}, mark it as not health", groupId);
      report = new HealthReport(false);
    }
    return report;
  }

  public int getStatus() {
    return this.status.get();
  }

  protected void setStatus(int statusCode) {
    this.status.set(statusCode);
  }

  public boolean isBusy() {
    return this.status.get() != IDLE;
  }

  protected void setSpare() {
    this.status.set(IDLE);
  }

  public boolean isShutDownAsync() {
    return shutDownAsync;
  }

  public void setShutDownAsync(boolean shutDownAsync) {
    this.shutDownAsync = shutDownAsync;
  }

  /***********************Core Interface: Open or recover interpreter*****************************/

  @InterpreterLifeCycle
  @TransactionGuarantee
  public synchronized Interpreter getOrCreateInterpreter(String className, Properties prop)
          throws LifeCycleException, InterpreterException {
    Interpreter interpreter = getInterpreter(className);
    if (interpreter == null) {
      try {
          validateAndAddRule(className, userName);
          logger.info("Create interpreter: {}... for {}", className, userName);
          /** pass down the user context to the interpreter's construction**/
          prop.put("zeta.userId", userName);
          prop.put("zeta.noteId", noteId);
        try {
          interpreter = _factory.getBaseFactory().create(className, prop);
        } catch (Exception ex) {
          logger.info("Got error when construct interpreter, only remove rules");
          removeRule(className);
          throw ex;
        }
        interpreter.setInterpreterGroup(this);
        interpreter.setUserName(userName);
        interpreterMap.put(
                InterpreterClassInfo.of(
                        interpreter.getClass().getName(),
                        className,
                        interpreter.getClassName()),
                interpreter);
        interpreter.open();
        //recoverOrOpen(interpreter);
        getHealthReport().setHealth(true);
        logger.info("Interpreter created: {}", className);
      } catch (Exception e) {
        String msg = String.format("error when create interpreter: %s, remove it from " +
                "group: %s, ex: %s", className, groupId, ExceptionUtils.getFullStackTrace(e));
        logger.error(msg);
        // Remove interpreter and remove rules if interpreter is already in the map
        removeInterpreter(className);
        if (e instanceof InterpreterStoppedException) {
          throw (InterpreterStoppedException) e;
        } else if (e instanceof LifeCycleException) {
          throw (LifeCycleException) e;
        } else {
          String error = ExceptionUtils.getFullStackTrace(e);
          logger.error("User: {}, Note: {} got exception when open: {}", userName, noteId, error);
          throw new InterpreterException(ErrorCode.INTERPRETER_SERVICE_EXCEPTION, error);
        }
      }
    }
    return interpreter;
  }

  /***********************Core Interface: Open or recover interpreter*****************************/

  @InterpreterLifeCycle
  private void recoverOrOpen(Interpreter interpreter) throws Exception {
    if (interpreter instanceof Recoverable) {
      StateManager stateManager = _factory.getStateManager();
      if (stateManager.isEnabled()) {
        boolean recovered = ((Recoverable) interpreter).recover(stateManager);
        if (recovered) {
         logger.info("Successfully recovered interpreter: {}", interpreter.getClassName());
        } else {
          logger.info("Failed to recover interpreter: {}, just open it",
                  interpreter.getClassName());
          interpreter.open();
          // persist the state of the interpreter async
          EventTracker.postEvent(ZetaStatePersistEvent(userName, noteId, interpreter));
        }
      } else {
        logger.info("This is a recoverable interpreter: {}, but state recover is not enabled, open it directly",
                interpreter.getClassName());
        interpreter.open();
      }
    } else {
      logger.info("This is not a recoverable interpreter: {}, open it directly", interpreter.getClassName());
      interpreter.open();
    }
  }

  @InterpreterLifeCycle
  @ShouldBeQuick
  private void unPersist(Interpreter interpreter) {
    if (interpreter instanceof Recoverable) {
      StateManager manager = _factory.getStateManager();
      if (manager.isEnabled()) {
        manager.destroyStateSnapshot(((Recoverable) interpreter).recoverKey());
      } else {
        logger.info("State recover is not enabled, don't need to unPersist");
      }
    } else {
      logger.info("Not a recoverable interpreter: {}, no need to unPersist", interpreter.getClassName());
    }
  }

  @InterpreterLifeCycle
  private void validateAndAddRule(String className, String userId) throws LifeCycleException {
    boolean isMonitored = _factory != null && _factory.isMonitored();
    if (isMonitored) {
      LifeCycleManager lifeCycleManager = _factory.getLifeCycleManager();
      logger.info("Validate user");
      if (lifeCycleManager.whiteListClassName(className)) {
        logger.info(className + " is in the white list, skip validation");
        return;
      }
      if (lifeCycleManager.whiteListNoteId(noteId)) {
        logger.info(noteId + " is in the white list, don't need to remove rule");
        return;
      }
      if (lifeCycleManager.filtered(userId)) {
        throw new LifeCycleException(lifeCycleManager.explainUserRules(userId));
      } else {
        lifeCycleManager.addRule(userId);
      }
    }
  }

  @InterpreterLifeCycle
  private void removeRule(InterpreterClassInfo classInfo) {
    removeRule(classInfo.getStaticClassName());
  }

  @InterpreterLifeCycle
  private void removeRule(String staticClassName) {
    boolean isMonitored = _factory != null && _factory.isMonitored();
    if (isMonitored) {
      LifeCycleManager lifeCycleManager = _factory.getLifeCycleManager();
      logger.info("Removing one rule for user: " +
              this.userName +
              " with interpreter: " + staticClassName +
              " in group: " + this.groupId);
      if (lifeCycleManager.whiteListClassName(staticClassName)) {
        logger.info(staticClassName + " is in the white list, don't need to remove rule");
        return;
      }
      if (lifeCycleManager.whiteListNoteId(noteId)) {
        logger.info(noteId + " is in the white list, don't need to remove rule");
        return;
      }
      lifeCycleManager.removeRule(this.userName);
      logger.info("One rule removed from user");
    }
  }

  public void addInterpreter(String className, Interpreter intp) {
    interpreterMap.put(InterpreterClassInfo.of(intp.getClass().getName(), className, intp.getClassName()), intp);
  }

  private Optional<InterpreterClassInfo> getClassInfo(String className) {
    return interpreterMap.keySet().stream()
            .filter(k -> k.isValid(className)).findFirst();
  }

  /***********************Core Interface: Close interpreter*****************************/

  private void closeAsyncOrSync(Runnable closeRunnable) {
    if (isShutDownAsync()) {
      new Thread(closeRunnable).start();
    } else {
      try {
        closeRunnable.run();
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }

  @InterpreterLifeCycle
  @TransactionGuarantee
  @ShouldBeQuick
  public void removeInterpreter(String className) {
    try {
      Optional<InterpreterClassInfo> classInfo = getClassInfo(className);
      if (classInfo.isPresent()) {
        Interpreter intp = interpreterMap.remove(classInfo.get());
        removeRule(classInfo.get());
        closeAsyncOrSync(() -> {
          intp.pause();
          intp.cancelAll();
          intp.destroy();
        });
        //unPersist(intp);
      }
    } finally {
      if (interpreterMap.size() == 0) {
        getHealthReport().setHealth(false);
      }
    }
  }

  @InterpreterLifeCycle
  @TransactionGuarantee
  @ShouldBeQuick
  public void shutdownAllInterpreters() {
    try {
      for (InterpreterClassInfo classInfo : interpreterMap.keySet()) {
        Interpreter intp = interpreterMap.get(classInfo);
        removeRule(classInfo);
        closeAsyncOrSync(() -> {
          intp.pause();
          intp.destroy();
        });
        //unPersist(intp);
      }
    } finally {
      getHealthReport().setHealth(false);
    }
  }

  @InterpreterLifeCycle
  @TransactionGuarantee
  @ShouldBeQuick
  public void safeClose() {
    try {
      for (InterpreterClassInfo classInfo : interpreterMap.keySet()) {
        Interpreter intp = interpreterMap.get(classInfo);
        removeRule(classInfo);
        closeAsyncOrSync(() -> {
          intp.pause();
          intp.cancelAll();
          intp.destroy();
        });
        //unPersist(intp);
      }
    } finally {
      getHealthReport().setHealth(false);
    }
  }

  @InterpreterLifeCycle
  @TransactionGuarantee
  @ShouldBeQuick
  public void clear() {
    safeClose();
    interpreterMap.clear();
  }

  /***********************Core Interface: Close interpreter*****************************/

  public void doGc() {
    for (Interpreter intp : interpreterMap.values()) {
      try {
        intp.doGc();
      } catch (Exception e) {
        logger.error("Gc failed of interpreter group : " + groupId + " by: " + e.toString());
        e.printStackTrace();
      }
    }
  }

  public boolean isAllOpened() {
    for (Interpreter intp : interpreterMap.values()) {
      try {
        if (!intp.isOpened()) {
          return false;
        }
      } catch (Exception e) {
        logger.error("Check interpreter status failed of interpreter group " + groupId, e);
      }
    }
    return true;
  }

  public boolean hasOpened() {
    for (Interpreter intp : interpreterMap.values()) {
      try {
        if (intp.isOpened()) {
          return true;
        }
      } catch (Exception e) {
        logger.error("Check interpreter status failed of interpreter group : " + groupId + " by ", e);
      }
    }
    return false;
  }

  public String toJson() {
    String status = StatusCode.getNameFromCode(getStatus());
    return "{\"userName\": \"" + userName + "\", \"noteId\": \"" + noteId + "\", \"status\": \"" + status + "\", \"interpreterInstances\": [" +
            (interpreterMap.size() == 0 ? "" : interpreterMap
                    .values()
                    .stream()
                    .map(Interpreter::toJson).collect(Collectors.joining(","))) +
            "]}";
  }

  public String getGroupId() {
    return this.groupId;
  }

  public Collection<Interpreter> getInterpreters() {
    return this.interpreterMap.values();
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getNoteId() {
    return noteId;
  }

  public void setNoteId(String noteId) {
    this.noteId = noteId;
  }

  public InterpreterFactory getInterpreterFactory() {
    return _factory;
  }

  public void setInterpreterFactory(InterpreterFactory _factory) {
    this._factory = _factory;
  }

  public int getInterpreterNums() {
    return getInterpreters().size();
  }

  public static String makeGrpKey(String userId, String noteId) {
    return InterpreterFactory.getGrpKey(userId, noteId);
  }

  public void destroy(boolean forceShutDown) {
    this._factory.removeInterpreterGroup(this.groupId, forceShutDown);
  }

  public static class StatusCode {
    public static final int NOT_EXISTS = -2;
    public static final int CREATED = -1;
    public static final int IDLE = 0;
    public static final int STARTING = 1;
    public static final int RUNNING = 2;

    public static String getNameFromCode(int code) {
      String name;
      switch (code) {
        case CREATED:
          name = "CREATED";
          break;
        case STARTING:
          name = "STARTING";
          break;
        case IDLE:
          name = "IDLE";
          break;
        case RUNNING:
          name = "RUNNING";
          break;
        case NOT_EXISTS:
          name = "NOT_EXISTS";
          break;
        default:
          name = "UNKNOWN";
          break;

      }
      return name;
    }

    public static String mapToZetaStatus(int code) {
      String zetaStatus;
      switch (code) {
        case NOT_EXISTS:
        case CREATED:
          zetaStatus = "Disconnected";
          break;
        case STARTING:
          zetaStatus = "Connecting";
          break;
        case IDLE:
        case RUNNING:
          zetaStatus = "Connected";
          break;
        default:
          zetaStatus = "Disconnected";
          break;
      }
      return zetaStatus;
    }
  }

  private static class InterpreterClassInfo {

    private String jvmInstanceClassName;
    private String jvmClassName;
    private String interpreterSelfDeclaredClassName;

    InterpreterClassInfo(String jvmInstanceClassName, String jvmClassName, String interpreterSelfDeclaredClassName) {
      this.jvmInstanceClassName = jvmInstanceClassName;
      this.jvmClassName = jvmClassName;
      this.interpreterSelfDeclaredClassName = interpreterSelfDeclaredClassName;
    }

    static InterpreterClassInfo of(String jvmInstanceClassName, String jvmClassName, String interpreterSelfDeclaredClassName) {
      return new InterpreterClassInfo(jvmInstanceClassName, jvmClassName, interpreterSelfDeclaredClassName);
    }

    public boolean isValid(String claslName) {
      return Objects.equals(jvmInstanceClassName, claslName) ||
              Objects.equals(jvmClassName, claslName) ||
              Objects.equals(interpreterSelfDeclaredClassName, claslName);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;

      if (o == null || getClass() != o.getClass()) return false;

      InterpreterClassInfo that = (InterpreterClassInfo) o;

      return new EqualsBuilder()
              .append(jvmInstanceClassName, that.jvmInstanceClassName)
              .append(jvmClassName, that.jvmClassName)
              .append(interpreterSelfDeclaredClassName, that.interpreterSelfDeclaredClassName)
              .isEquals();
    }

    @Override
    public int hashCode() {
      return new HashCodeBuilder(17, 37)
              .append(jvmInstanceClassName)
              .append(jvmClassName)
              .append(interpreterSelfDeclaredClassName)
              .toHashCode();
    }

    public String getJvmInstanceClassName() {
      return jvmInstanceClassName;
    }

    public void setJvmInstanceClassName(String jvmInstanceClassName) {
      this.jvmInstanceClassName = jvmInstanceClassName;
    }

    public String getStaticClassName() {
      return jvmClassName;
    }

    public void setJvmClassName(String jvmClassName) {
      this.jvmClassName = jvmClassName;
    }

    public String getInterpreterSelfDeclaredClassName() {
      return interpreterSelfDeclaredClassName;
    }

    public void setInterpreterSelfDeclaredClassName(String interpreterSelfDeclaredClassName) {
      this.interpreterSelfDeclaredClassName = interpreterSelfDeclaredClassName;
    }

    @Override
    public String toString() {
      return "InterpreterClassInfo{" +
              "jvmInstanceClassName='" + jvmInstanceClassName + '\'' +
              ", jvmClassName='" + jvmClassName + '\'' +
              ", interpreterSelfDeclaredClassName='" + interpreterSelfDeclaredClassName + '\'' +
              '}';
    }
  }
}
