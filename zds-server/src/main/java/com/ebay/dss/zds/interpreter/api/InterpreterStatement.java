package com.ebay.dss.zds.interpreter.api;

import com.ebay.dss.zds.common.JsonUtil;
import com.ebay.dss.zds.interpreter.InterpreterType;
import com.ebay.dss.zds.interpreter.input.ExecutionContext;
import com.ebay.dss.zds.interpreter.interpreters.Interpreter;
import com.ebay.dss.zds.interpreter.listener.InterpreterListener;
import com.ebay.dss.zds.interpreter.listener.InterpreterListenerData;
import com.ebay.dss.zds.interpreter.listener.InterpreterZetaBaseListener;
import com.ebay.dss.zds.interpreter.monitor.modle.Status;
import com.ebay.dss.zds.interpreter.output.InterpreterResultBuilder;
import com.ebay.dss.zds.interpreter.output.result.Result;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.micrometer.core.instrument.util.StringUtils;
import org.apache.zeppelin.interpreter.InterpreterResult;
import org.codehaus.plexus.util.ExceptionUtils;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by tatian on 2021/3/30.
 */
public class InterpreterStatement implements Serializable {

  @JsonIgnore
  public static transient AtomicLong nextStatementId = new AtomicLong(0);
  public final long id;
  private String code;
  @JsonIgnore
  private transient ExecutionContext context;
  private InterpreterResult interpreterResult;
  private volatile StatementStatus status;
  @JsonIgnore
  private transient InterpreterSession session;
  private volatile String progress = "Waiting";
  private long createTime;
  private long startTime;
  private long endTime;

  public InterpreterStatement(ExecutionContext context, InterpreterSession session) {
    this.id = nextStatementId.getAndIncrement();
    this.context = context;
    this.code = context.getCode();
    this.session = session;
    this.status = StatementStatus.CREATED;
    this.createTime = new Date().getTime();
  }

  public InterpreterResult getInterpreterResult() {
    return interpreterResult;
  }

  public void setInterpreterResult(InterpreterResult interpreterResult) {
    this.interpreterResult = interpreterResult;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public StatementStatus getStatus() {
    return status;
  }

  public void setStatus(StatementStatus status) {
    this.status = status;
  }

  public ExecutionContext getContext() {
    return context;
  }

  public String getProgress() {
    return progress;
  }

  public void setProgress(String progress) {
    this.progress = progress;
  }

  public void execute() {

    if (isFinished()) {
      return;
    }

    try {
      setStatus(StatementStatus.RUNNING);
      setProgress("0.0%");
      this.startTime = System.currentTimeMillis();
      InterpreterResult result = session.execute(getContext(), new StatementListener(this, getListenerType()));
      this.endTime = System.currentTimeMillis();
      setProgress("100.0%");
      setInterpreterResult(result);
      setStatus(StatementStatus.fromInterpreterResult(result));
    } catch (Exception ex) {
      this.endTime = System.currentTimeMillis();
      setProgress("100.0%");
      setStatus(StatementStatus.ERROR);
      setInterpreterResult(new InterpreterResultBuilder().error().because(ex.getMessage()));
    }
  }

  public boolean isFinished() {
    return this.status == StatementStatus.SUCCESS ||
            this.status == StatementStatus.ERROR ||
            this.status == StatementStatus.CANCELED;
  }

  public void cancel() {
    if (!isFinished()) {
      try {
        this.session.cancelAll();
      } finally {
        // don't need to record the end time here, it will be recorded in execute function
        setStatus(StatementStatus.CANCELED);
      }
    }
  }

  public String toJson() {
    return JsonUtil.GSON.toJson(this);
  }

  public static InterpreterStatement fromJson(String json) {
    return JsonUtil.GSON.fromJson(json, InterpreterStatement.class);
  }

  protected InterpreterListener.ListenerType getListenerType() {
    InterpreterType.EnumType type = InterpreterType.fromString(this.session.getInterpreterName());
    return InterpreterZetaBaseListener.getListenerType(type);
  }

  private static class StatementListener implements InterpreterListener {

    private DateTime startDt;
    private DateTime endDt;
    private InterpreterStatement statement;
    private ListenerType listenerType;

    public StatementListener(InterpreterStatement statement, ListenerType listenerType) {
      this.statement = statement;
      this.listenerType = listenerType;
    }

    @Override
    public void beforeStatementSubmit(InterpreterListenerData interpreterListenerData) {

    }

    @Override
    public Long afterStatementSubmit(InterpreterListenerData interpreterListenerData) {
      startDt = new DateTime();
      return 0L;
    }

    @Override
    public void statementProgress(InterpreterListenerData interpreterListenerData, Status status) {
      this.statement.setProgress((status.getProgress() * 100) + "%");
    }

    @Override
    public void afterStatementFinish(InterpreterListenerData interpreterListenerData, Result result) {
      endDt = new DateTime();
    }

    @Override
    public void handleCancelling(InterpreterListenerData interpreterListenerData) {
      endDt = new DateTime();
    }

    @Override
    public ListenerType getListenerType() {
      return this.listenerType;
    }

    @Override
    public void setListenerType(ListenerType type) {
      this.listenerType = type;
    }

    @Override
    public long getCurrentStatementKey() {
      return 0;
    }

    @Override
    public DateTime getStartDt() {
      return this.startDt;
    }

    @Override
    public DateTime getEndDt() {
      return this.endDt;
    }
  }

}
