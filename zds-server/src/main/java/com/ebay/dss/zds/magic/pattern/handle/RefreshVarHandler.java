package com.ebay.dss.zds.magic.pattern.handle;

import com.ebay.dss.zds.common.QueueDestination;
import com.ebay.dss.zds.dao.ZetaNotebookRepository;
import com.ebay.dss.zds.interpreter.input.ExecutionContext;
import com.ebay.dss.zds.interpreter.interpreters.Interpreter;
import com.ebay.dss.zds.interpreter.listener.InterpreterListener;
import com.ebay.dss.zds.interpreter.listener.InterpreterListenerData;
import com.ebay.dss.zds.interpreter.output.InterpreterResultBuilder;
import com.ebay.dss.zds.interpreter.output.result.JsonResult;
import com.ebay.dss.zds.magic.*;
import com.ebay.dss.zds.magic.exception.ZetaMagicHandleException;
import com.ebay.dss.zds.magic.pattern.DynamicReplaceMatcher;
import com.ebay.dss.zds.magic.pattern.PatternHandler;
import com.ebay.dss.zds.model.NotebookVariable;
import com.ebay.dss.zds.model.NotebookVarsMap;
import com.ebay.dss.zds.model.ZetaNotebook;
import com.ebay.dss.zds.model.ZetaNotebookPreference;
import com.ebay.dss.zds.websocket.WebSocketResp;
import com.ebay.dss.zds.websocket.notebook.dto.CodeWithSeq;
import com.ebay.dss.zds.websocket.notebook.dto.NotebookVarsRefresh;
import org.apache.commons.lang.StringUtils;
import org.apache.zeppelin.interpreter.InterpreterResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static com.ebay.dss.zds.magic.DynamicVariableHandler.matchAndReplaceVars;
import static com.ebay.dss.zds.magic.pattern.MagicPattern.ZETA_MAGIC_REFRESH_VAR;
import static com.ebay.dss.zds.model.NotebookVariable.PATTERN_A;
import static com.ebay.dss.zds.model.NotebookVariable.PATTERN_B;

/**
 * Created by tatian on 2020-11-17.
 */
public class RefreshVarHandler implements PatternHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(RefreshVarHandler.class);

  @Override
  public HandleStateContext handle(HandleContext handleContext) throws ZetaMagicHandleException {
    String matchedStr = handleContext.matcher.group();
    String varName = ZETA_MAGIC_REFRESH_VAR.innerContent(matchedStr);
    final String trimmedVarName = NotebookVariable.getTrimmedVariableName(varName);
    NotebookVarsMap varsMap = handleContext.varsMap;
    NotebookVariable variable = varsMap.get(trimmedVarName);
    Interpreter interpreter = handleContext.interpreter;
    CodeWithSeq codeWithSeq = handleContext.codeWithSeq;

    if (variable == null) {
      String error = String.format("Can not find any variable: [%s] record in current notebook, maybe it has not been synced with backend", trimmedVarName);
      LOGGER.error(error);
      return HandleStateContext.failed(error);
      // throw new ZetaMagicHandleException(error);
    }

    if (StringUtils.isEmpty(variable.vargenerator)) {
      String error = String.format("Can not find any generator for variable: [%s], please set one in the Variables Panel", trimmedVarName);
      LOGGER.error(error);
      return HandleStateContext.failed(error);
      // throw new ZetaMagicHandleException(error);
    }

    HandleState state = HandleState.MAGIC;
    ExecutionAgent agent;
    // check if it's a variable references
    List<String> unTrimmedMatchedVariableRefs = NotebookVariable.findAllVariables(variable.vargenerator);
    // SQL generator
    if (unTrimmedMatchedVariableRefs.size() == 0) {
      handleContext.codeWithSeq.setCode(variable.vargenerator);
      agent = resolveSQLWithinVargenerator(handleContext, variable, interpreter, varsMap, unTrimmedMatchedVariableRefs);
      LOGGER.info("Variable: {} find vargenerator as: {}", trimmedVarName, variable.vargenerator);
    } else {

      if (unTrimmedMatchedVariableRefs.size() == 1) {
        String unTrimmed = unTrimmedMatchedVariableRefs.get(0);
        if (checkIfDirectRefer(unTrimmed, variable.vargenerator)) {
          String picked = NotebookVariable.getTrimmedVariableName(unTrimmed);
          // direct refer
          agent = variableReferringAgent(interpreter, trimmedVarName, picked, codeWithSeq, varsMap);
          LOGGER.info("Variable: {} direct referred to another variable: {}", trimmedVarName, picked);
        } else {
          // sql refer
          agent = resolveSQLWithinVargenerator(handleContext, variable, interpreter, varsMap, unTrimmedMatchedVariableRefs);
          LOGGER.info("Variable: {} find vargenerator as: {}", trimmedVarName, variable.vargenerator);
        }
      } else {
        // sql refer
        agent = resolveSQLWithinVargenerator(handleContext, variable, interpreter, varsMap, unTrimmedMatchedVariableRefs);
        LOGGER.info("Variable: {} find vargenerator as: {}", trimmedVarName, variable.vargenerator);
      }
    }
    return new HandleStateContext(state,
            agent,
            (JsonResult successResult,
             QueueDestination dest,
             ZetaNotebook notebook) -> persistVarValueAndNotify(trimmedVarName, varsMap, successResult, dest, notebook, handleContext.repository));
  }

  private boolean checkIfDirectRefer(String unTrimmedVariable, String vargenerator) {
    return unTrimmedVariable.equals(StringUtils.trim(vargenerator));
  }

  private ExecutionAgent resolveSQLWithinVargenerator(HandleContext handleContext,
                                                      NotebookVariable variable,
                                                      Interpreter interpreter,
                                                      NotebookVarsMap varsMap,
                                                      List<String> unTrimmedVariablesWithinVarGenerator) {
    String replacedVargenerator = variable.vargenerator;
    if (unTrimmedVariablesWithinVarGenerator.size() > 0) {
      DynamicReplaceMatcher drMatcher = DynamicReplaceMatcher.matchAllAndReplace(
              variable.vargenerator,
              (String matchedVar) -> matchAndReplaceVars(matchedVar, varsMap),
              PATTERN_A, PATTERN_B);
      if (drMatcher.replaced) {
        replacedVargenerator = drMatcher.result;
        LOGGER.info("Variable: {} find vargenerator with variable within as: {}", variable.name, replacedVargenerator);
      }
    }
    handleContext.codeWithSeq.setCode(replacedVargenerator);
    return ExecutionAgent.forwardAgent(interpreter);
  }

  private ExecutionAgent variableReferringAgent(Interpreter interpreter,
                                                final String handledVariableName,
                                                final String referredVariableName,
                                                CodeWithSeq codeWithSeq,
                                                final NotebookVarsMap varsMap) {
    return (ExecutionContext context, InterpreterListener listener) -> {
      InterpreterListenerData listenerData = new InterpreterListenerData();
      listener.beforeStatementSubmit(listenerData);
      listener.afterStatementSubmit(listenerData);
      InterpreterResult interpreterResult;
      NotebookVariable referredVariable = varsMap.get(referredVariableName);
      if (referredVariable == null) {
        interpreterResult = new InterpreterResultBuilder()
                .error()
                .because(String.format("Can not find any variable: %s refer to", referredVariableName));
      } else {
        // don't need to set the value to the referred one now, it will be done in the call back
        List<String> header = Arrays.asList(handledVariableName);
        List<Object> row = Arrays.asList(referredVariable.value);
        List<List<Object>> rows = Arrays.asList(row);
        interpreterResult = new InterpreterResultBuilder()
                .success()
                .table(header, rows)
                .build();
      }
      JsonResult jsonResult = new JsonResult(interpreterResult);
      listener.afterStatementFinish(listenerData, jsonResult);
      return interpreterResult;
    };
  }

  private void persistVarValueAndNotify(
          String refreshedVarName,
          NotebookVarsMap runtimeVarsMap,
          JsonResult successResult,
          QueueDestination dest,
          ZetaNotebook notebook,
          ZetaNotebookRepository repository) {
    String successValue = successResult.getResultOfXY(0, 0);
    persistVarValueAndNotify(refreshedVarName, runtimeVarsMap, successValue, dest, notebook, repository);
  }

  private void persistVarValueAndNotify(
          String refreshedVarName,
          NotebookVarsMap runtimeVarsMap,
          String value,
          QueueDestination dest,
          ZetaNotebook notebook,
          ZetaNotebookRepository repository) {
    // update the DB
    ZetaNotebookPreference preference = ZetaNotebookPreference.fromJson(notebook.getPreference());
    if (value == null) {
      value = "";
      LOGGER.info("Got null variable result, make it as a blank string");
    }
    NotebookVariable variable = runtimeVarsMap.get(refreshedVarName);
    /** update the status in the memory first**/
    variable.setValue(value);
    /** update the status in the memory first**/
    if (preference.variables == null) {
      preference.variables = new HashMap<>();
    }
    preference.variables.put(refreshedVarName, value);
    if (preference.vargenerators == null) {
      preference.vargenerators = new HashMap<>();
    }
    preference.vargenerators.put(refreshedVarName, variable.vargenerator);
    repository.updateNotebookPreferenceByIdAndNt(notebook.getId(), preference.toJson(), notebook.getNt());
    LOGGER.info("Variable updated for note: {} with variable: {}, value: {}",
            notebook.getId(), refreshedVarName, value);
    // inform frontend
    NotebookVarsMap refreshVarsMap = new NotebookVarsMap();
    refreshVarsMap.put(refreshedVarName, variable);
    NotebookVarsRefresh refresh = new NotebookVarsRefresh(notebook.getId(), refreshVarsMap);
    WebSocketResp<NotebookVarsRefresh> resp = new WebSocketResp<>(WebSocketResp.OP.NB_VAR_REFRESH, refresh);
    dest.sendData(resp);
  }
}
