package com.ebay.dss.zds.interpreter.interpreters.imitation;

import com.ebay.dss.zds.interpreter.input.ExecutionContext;
import com.ebay.dss.zds.interpreter.interpreters.Interpreter;
import com.ebay.dss.zds.interpreter.interpreters.imitation.behavior.BehaviorSimulator;
import com.ebay.dss.zds.interpreter.interpreters.imitation.behavior.Do;
import com.ebay.dss.zds.interpreter.interpreters.imitation.behavior.Sleep;
import com.ebay.dss.zds.interpreter.listener.InterpreterListener;
import com.ebay.dss.zds.interpreter.listener.InterpreterListenerData;
import com.ebay.dss.zds.interpreter.output.InterpreterResultBuilder;
import com.ebay.dss.zds.interpreter.output.result.JsonResult;
import org.apache.zeppelin.interpreter.InterpreterResult;
import org.codehaus.plexus.util.ExceptionUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by tatian on 2020-12-02.
 */
public class ImitationInterpreter extends Interpreter {

  public static final String OPEN_TIME = "zds.imitation.behavior.open.openTime";
  public static final String RESULT_COLUMN_NUMS = "zds.imitation.result.column.nums";
  public static final String RESULT_ROW_NUMS = "zds.imitation.result.row.nums";

  private BehaviorListener behaviorListener;
  private AtomicBoolean opened = new AtomicBoolean(false);
  private volatile AtomicReference<Behavior> inFlight = new AtomicReference<>();
  private static AtomicInteger sessionId = new AtomicInteger(0);
  private AtomicInteger statementId = new AtomicInteger(0);
  private volatile int currentSessionId = -1;
  private final int resultColumnNums;
  private final int resultRowNums;

  public ImitationInterpreter(Properties properties) {
    super(properties);
    this.resultColumnNums = Integer.valueOf(properties.getProperty(RESULT_COLUMN_NUMS, "10"));
    this.resultRowNums = Integer.valueOf(properties.getProperty(RESULT_ROW_NUMS, "1000"));
  }

  @Override
  public boolean isOpened() {
    return opened.get();
  }

  @Override
  protected void construct() throws Exception {
    this.behaviorListener = BehaviorListenerFactory.create(getProperties());
    Behavior behavior = behaviorListener.onConstruct(new Properties());
    this.inFlight.set(behavior);
    behavior.perform();
    currentSessionId = sessionId.getAndIncrement();
    opened.set(true);
  }

  @Override
  public void close() {
    try {
      Optional.ofNullable(inFlight.get()).ifPresent(Behavior::stop);
    } finally {
      opened.set(false);
    }
  }

  @Override
  protected InterpreterResult interpret(String code, String paragraphId) {
    try {
      Behavior behavior = behaviorListener.onInterpret(code, paragraphId);
      logger.info(behavior.getClass().getName());
      this.inFlight.set(behavior);
      return tryPerform(behavior);
    } finally {
      this.inFlight.set(null);
    }
  }

  @Override
  protected InterpreterResult interpret(String code, String paragraphId, InterpreterListener listener) {
    try {
      InterpreterListenerData interpreterListenerData = new InterpreterListenerData();
      interpreterListenerData.setStatementId(this.statementId.getAndIncrement());
      interpreterListenerData.setSessionId(currentSessionId);
      listener.beforeStatementSubmit(interpreterListenerData);
      Behavior behavior = behaviorListener.onInterpret(code, paragraphId, listener);
      logger.info(behavior.getClass().getName());
      this.inFlight.set(behavior);
      listener.afterStatementSubmit(interpreterListenerData);
      InterpreterResult result = tryPerform(behavior);
      listener.afterStatementFinish(interpreterListenerData, new JsonResult(result));
      return result;
    } finally {
      this.inFlight.set(null);
    }
  }

  @Override
  protected InterpreterResult doExecute(ExecutionContext context) {
    return interpret(context.getCode(), context.getParagraphId() + "");
  }

  @Override
  protected InterpreterResult doExecute(ExecutionContext context, InterpreterListener listener) {
    return this.interpret(context.getCode(), context.getParagraphId() + "", listener);
  }

  @Override
  public void cancel(ExecutionContext context) {
    cancelAll();
  }

  @Override
  public List<Integer> cancelAll() {
    Optional.ofNullable(inFlight.get()).ifPresent(Behavior::stop);
    return new ArrayList<>();
  }

  @Override
  public FormType getFormType() {
    return null;
  }

  @Override
  public int getProgress(ExecutionContext context) {
    return Optional.ofNullable(inFlight.get()).map(Behavior::progress).orElse(0);
  }

  @Override
  public void doGc() {

  }

  private InterpreterResult tryPerform(Behavior behavior) {
    try {
      behavior.perform();
      return success();
    } catch (Exception ex) {
      String reason = ExceptionUtils.getFullStackTrace(ex);
      logger.error(reason);
      return failed(reason);
    }
  }

  private InterpreterResult failed(String reason) {
    return new InterpreterResultBuilder().error().because(reason);
  }

  private InterpreterResult success() {
    List<String> header = IntStream.rangeClosed(1, this.resultColumnNums).boxed().map(i -> "h" + i).collect(Collectors.toList());
    List<List<Object>> rows = IntStream.range(1, this.resultRowNums).boxed()
            .map(i -> IntStream.rangeClosed(1, this.resultColumnNums).boxed().map(j -> (Object) ("v" + i + "_" + j)).collect(Collectors.toList()))
            .collect(Collectors.toList());

    return new InterpreterResultBuilder()
            .success()
            .table(header, rows)
            .build();
  }

  public static class ImitationBehaviorListener implements BehaviorListener {

    private Properties properties;

    public ImitationBehaviorListener(Properties properties) {
      this.properties = properties;
    }

    public Behavior onConstruct(Properties context) {
      return new Sleep(Long.valueOf(properties.getProperty(OPEN_TIME, "1000")));
    }

    public Behavior onInterpret(String code, String paragraphId) {
      try {
        return BehaviorSimulator.simulate(code.trim());
      } catch (Exception ex) {
        ex.printStackTrace();
        return Do.DoNothing();
      }
    }

    public Behavior onInterpret(String code, String paragraphId, InterpreterListener listener) {
      return onInterpret(code, paragraphId);
    }
  }
}
