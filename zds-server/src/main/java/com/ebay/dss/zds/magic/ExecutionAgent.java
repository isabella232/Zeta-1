package com.ebay.dss.zds.magic;

import com.ebay.dss.zds.interpreter.input.ExecutionContext;
import com.ebay.dss.zds.interpreter.interpreters.Interpreter;
import com.ebay.dss.zds.interpreter.listener.InterpreterListener;
import com.ebay.dss.zds.interpreter.listener.InterpreterListenerData;
import com.ebay.dss.zds.interpreter.output.InterpreterResultBuilder;
import com.ebay.dss.zds.interpreter.output.result.JsonResult;
import org.apache.zeppelin.interpreter.InterpreterResult;

/**
 * Created by tatian on 2020-11-17.
 */
public interface ExecutionAgent {
  InterpreterResult execute(ExecutionContext context, InterpreterListener listener);

  static ExecutionAgent forwardAgent(Interpreter interpreter) {
    return interpreter != null ? interpreter::execute :
            (ExecutionContext context, InterpreterListener listener) -> new InterpreterResultBuilder()
                    .error()
                    .because("Empty interpret");
  }

  static ExecutionAgent failedExecutionAgent(final String errorMessage) {
    return (ExecutionContext context, InterpreterListener listener) -> {
      InterpreterListenerData listenerData = new InterpreterListenerData();
      listener.beforeStatementSubmit(listenerData);
      listener.afterStatementSubmit(listenerData);
      InterpreterResult interpreterResult = new InterpreterResultBuilder()
              .error()
              .because(errorMessage);
      JsonResult jsonResult = new JsonResult(interpreterResult);
      listener.afterStatementFinish(listenerData, jsonResult);
      return interpreterResult;
    };
  }
}
