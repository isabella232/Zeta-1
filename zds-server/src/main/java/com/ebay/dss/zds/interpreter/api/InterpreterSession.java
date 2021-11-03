package com.ebay.dss.zds.interpreter.api;

import com.ebay.dss.zds.common.JsonUtil;
import com.ebay.dss.zds.magic.ParserUtils;
import com.ebay.dss.zds.exception.IllegalStatusException;
import com.ebay.dss.zds.exception.InterpreterSessionNotReadyException;
import com.ebay.dss.zds.interpreter.InterpreterFactory;
import com.ebay.dss.zds.interpreter.InterpreterType;
import com.ebay.dss.zds.interpreter.input.ExecutionContext;
import com.ebay.dss.zds.interpreter.interpreters.Interpreter;
import com.ebay.dss.zds.interpreter.listener.InterpreterListener;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.zeppelin.interpreter.InterpreterResult;
import org.codehaus.plexus.util.ExceptionUtils;
import org.datanucleus.util.StringUtils;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by tatian on 2021/3/30.
 */
public class InterpreterSession implements Serializable {

  public final String sessionId;
  public final String user;
  public final String noteId;
  private String interpreterName;
  private transient InterpreterCreator creator;
  private transient Interpreter interpreter;
  private transient Runnable closeHook;
  private volatile String comment = "No comment";
  private volatile InterpreterSessionStatus status = InterpreterSessionStatus.NOT_READY;
  private transient AtomicLong innerIndex = new AtomicLong(0);
  private transient volatile long lastFinishTime = 0L;
  private final transient Object lock = new Object();
  private final transient ReentrantLock statusLock = new ReentrantLock();

  private transient Cache<Long, InterpreterStatement> statements;

  public InterpreterSession(Interpreter interpreter) {
    this.sessionId = interpreter.getInterpreterGroup().getGroupId();
    this.user = interpreter.getInterpreterGroup().getUserName();
    this.noteId = interpreter.getInterpreterGroup().getNoteId();
    this.interpreter = interpreter;
    this.interpreterName = InterpreterType.EnumType.getInterpreterName(interpreter.getClass());
    validateInterpreterInfo();
    this.statements = buildCache();
    this.status = InterpreterSessionStatus.IDLE;
  }

  public InterpreterSession(String user, String noteId, String interpreterName, InterpreterCreator creator) {
    this.sessionId = InterpreterFactory.getGrpKey(user, noteId);
    this.user = user;
    this.noteId = noteId;
    this.interpreterName = interpreterName;
    this.creator = creator;
  }

  public InterpreterSession(String user, String noteId, String interpreterName) {
    this.sessionId = InterpreterFactory.getGrpKey(user, noteId);
    this.user = user;
    this.noteId = noteId;
    this.interpreterName = interpreterName;
  }

  public void setCreator(InterpreterCreator creator) {
    this.creator = creator;
  }

  public void setCloseHook(Runnable runnable) {
    this.closeHook = runnable;
  }

  private void validateInterpreterInfo() {
    if (StringUtils.isEmpty(this.interpreterName)) {
      this.comment = "Set an unregister interpreter: " + interpreter.getClassName();
      setStatus(InterpreterSessionStatus.ERROR);
      refreshLastFinish();
      throw new IllegalStatusException(this.comment);
    }
  }

  private void refreshLastFinish() {
    this.lastFinishTime = System.currentTimeMillis();
  }

  private void setStatus(InterpreterSessionStatus status) {
    try {
      statusLock.lock();
      this.status = status;
    } finally {
      statusLock.unlock();
    }
  }

  public synchronized void initialize() throws Exception {
    if (this.interpreter != null) throw new IllegalStatusException("The interpreter within this session already set");
    try {
      setStatus(InterpreterSessionStatus.CREATING);
      if (this.creator == null) throw new IllegalStatusException("The interpreter creator can not be null");
      this.interpreter = this.creator.create();
      this.statements = buildCache();
      setStatus(InterpreterSessionStatus.IDLE);
    } catch (Exception ex) {
      this.comment = ExceptionUtils.getFullStackTrace(ex);
      setStatus(InterpreterSessionStatus.ERROR);
      refreshLastFinish();
      // don't call this here,
      //this.creator.destroy(true);
      throw ex;
    }
  }

  public void readyWithInterpreter(Interpreter interpreter) throws IllegalStatusException {
    if (this.interpreter != null) throw new IllegalStatusException("The interpreter within this session already set");
    this.interpreter = interpreter;
    this.interpreterName = InterpreterType.EnumType.getInterpreterName(interpreter.getClass());
    validateInterpreterInfo();
    this.statements = buildCache();
    setStatus(InterpreterSessionStatus.IDLE);
  }

  protected Interpreter getInterpreter() {
    return this.interpreter;
  }

  @Deprecated
  public InterpreterSession() {
    this.sessionId = "";
    this.user = null;
    this.noteId = null;
    this.interpreter = null;
    this.statements = null;
  }

  private <T> Cache<Long, T> buildCache() {

    CacheBuilder builder = CacheBuilder.newBuilder();
    builder.expireAfterAccess(60 * 60, TimeUnit.SECONDS);
    builder.initialCapacity(1);
    builder.maximumSize(5);
    builder.concurrencyLevel(200);

    return builder.build();
  }

  public InterpreterStatement prepareStatement(String code) throws InterpreterSessionNotReadyException {
    if (getStatus() == InterpreterSessionStatus.NOT_READY) throw new InterpreterSessionNotReadyException(this.sessionId);
    String parsed = parseCode(code);
    ExecutionContext context = new ExecutionContext(this.noteId, 0, interpreter.getClassName(), parsed);
    String currentIndex = String.valueOf(innerIndex.getAndIncrement());
    context.setJobId(currentIndex);
    context.setRequestId(currentIndex);
    context.setProp(this.interpreter.getProperties());
    InterpreterStatement statement = new InterpreterStatement(context, this);
    statements.put(statement.id, statement);
    return statement;
  }

  private String parseCode(String code) {
    List<String> sqls = new ParserUtils.SplitSQL(code)
            .trimResult()
            .omitEmptyStrings()
            .removeComment()
            .get();
    if (sqls != null && sqls.size() > 0) {
      return sqls.get(0);
    } else {
      return code;
    }
  }

  // todo: consider the concurrency mode
  protected InterpreterResult execute(ExecutionContext context, InterpreterListener listener) {
    synchronized (lock) {
      try {
        setStatus(InterpreterSessionStatus.BUSY);
        return this.interpreter.execute(context, listener);
      } finally {
        setStatus(InterpreterSessionStatus.IDLE);
      }
    }
  }

  protected InterpreterResult execute(ExecutionContext context) {
    synchronized (lock) {
      try {
        setStatus(InterpreterSessionStatus.BUSY);
        return this.interpreter.execute(context);
      } finally {
        setStatus(InterpreterSessionStatus.IDLE);
      }
    }
  }

  public Optional<InterpreterStatement> getStatement(Long id) {
    return Optional.ofNullable(this.statements.getIfPresent(id));
  }

  public void cancelAll() {
    try {
      this.interpreter.cancelAll();
    } finally {
      setStatus(InterpreterSessionStatus.IDLE);
    }
  }

  public void close(boolean async) {
    try {
      if (this.creator != null) {
        removeCloseHookWhenActiveClose();
        this.creator.destroy(async);
      }
    } finally {
      if (!this.isFinished()) {
        setStatus(InterpreterSessionStatus.CLOSED);
        refreshLastFinish();
      }
    }
  }

  private void removeCloseHookWhenActiveClose() {
    if (this.interpreter != null) {
      this.interpreter.removeHooks(this.closeHook);
    }
  }

  protected void markClosed() {
    setStatus(InterpreterSessionStatus.CLOSED);
    refreshLastFinish();
  }

  protected void markError(String error) {
    setStatus(InterpreterSessionStatus.ERROR);
    this.comment = error;
    refreshLastFinish();
  }

  public String toJson() {
    return JsonUtil.GSON.toJson(this);
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public InterpreterSessionStatus getStatus() {
    try {
      statusLock.lock();
      return status;
    } finally {
      statusLock.unlock();
    }
  }

  public String getInterpreterName() {
    return interpreterName;
  }

  public Class<?> getInterpreterClass() {
    return this.interpreter.getClass();
  }

  public boolean isHealthy() {
    try {
      statusLock.lock();
      return this.status != InterpreterSessionStatus.NOT_READY &&
              this.status != InterpreterSessionStatus.ERROR &&
              this.status != InterpreterSessionStatus.CLOSED;
    } finally {
      statusLock.unlock();
    }
  }

  public boolean isUsable() {
    try {
      statusLock.lock();
      return this.status == InterpreterSessionStatus.IDLE || this.status == InterpreterSessionStatus.BUSY;
    } finally {
      statusLock.unlock();
    }
  }

  public boolean isFinished() {
    try {
      statusLock.lock();
      return this.status == InterpreterSessionStatus.ERROR || this.status == InterpreterSessionStatus.CLOSED;
    } finally {
      statusLock.unlock();
    }
  }

  public static InterpreterSession fromJson(String json) {
    return JsonUtil.GSON.fromJson(json, InterpreterSession.class);
  }

  public InterpreterStatusInspector getInterpreterInspector() {
    return new InterpreterStatusInspector(this.interpreter);
  }

  public long getLastFinishTime() {
    return this.lastFinishTime;
  }

  public static class InterpreterStatusInspector {
    @NotNull
    private final Interpreter interpreter;
    public InterpreterStatusInspector(@NotNull Interpreter interpreter) {
      assert interpreter != null;
      this.interpreter = interpreter;
    }

    public boolean isOpen() {
      return this.interpreter.isOpened();
    }
  }

}
