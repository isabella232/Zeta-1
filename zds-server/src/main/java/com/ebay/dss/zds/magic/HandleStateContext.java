package com.ebay.dss.zds.magic;

import com.ebay.dss.zds.interpreter.interpreters.Interpreter;

import static com.ebay.dss.zds.magic.HandleState.NO;
import static com.ebay.dss.zds.magic.VariableHandlerCallback.DO_NOTHING;

/**
 * Created by tatian on 2020-11-17.
 */
public class HandleStateContext {

  public final HandleState handleState;
  public final ExecutionAgent executionAgent;
  public final VariableHandlerCallback callback;

  public HandleStateContext(HandleState handleState, ExecutionAgent executionAgent, VariableHandlerCallback callback) {
    this.handleState = handleState;
    this.executionAgent = executionAgent;
    this.callback = callback;
  }

  public HandleStateContext(HandleState handleState, ExecutionAgent executionAgent) {
    this(handleState, executionAgent, DO_NOTHING);
  }

  public static HandleStateContext failed(String message) {
    return new HandleStateContext(NO, ExecutionAgent.failedExecutionAgent(message));
  }

  public static HandleStateContext forwarded(Interpreter interpreter) {
    return new HandleStateContext(NO, ExecutionAgent.forwardAgent(interpreter));
  }
}
