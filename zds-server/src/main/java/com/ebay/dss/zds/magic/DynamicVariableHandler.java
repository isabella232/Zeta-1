package com.ebay.dss.zds.magic;

import com.ebay.dss.zds.dao.ZetaNotebookRepository;
import com.ebay.dss.zds.interpreter.interpreters.Interpreter;
import com.ebay.dss.zds.magic.exception.ZetaMagicHandleException;
import com.ebay.dss.zds.magic.exception.ZetaVariableInjectionException;
import com.ebay.dss.zds.magic.pattern.DynamicReplaceMatcher;
import com.ebay.dss.zds.magic.pattern.MagicPattern;
import com.ebay.dss.zds.magic.pattern.PatternHandler;
import com.ebay.dss.zds.model.NotebookVariable;
import com.ebay.dss.zds.model.NotebookVarsMap;
import com.ebay.dss.zds.model.ZetaNotebook;
import com.ebay.dss.zds.websocket.notebook.dto.CodeWithSeq;
import org.codehaus.plexus.util.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static com.ebay.dss.zds.model.NotebookVariable.PATTERN_A;
import static com.ebay.dss.zds.model.NotebookVariable.PATTERN_B;

/**
 * Created by tatian on 2020-10-22.
 */
@Component
public class DynamicVariableHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(DynamicVariableHandler.class);

  @Autowired
  private ZetaNotebookRepository repository;

  @Value("${zds.notebook.dynamicVariable.enable:#{true}}")
  private volatile boolean enabled;

  private HandleStateContext notHandled(Interpreter interpreter) {
    return HandleStateContext.forwarded(interpreter);
  }

  public final HandleStateContext handle(Interpreter interpreter, CodeWithSeq codeWithSeq, ZetaNotebook notebook, NotebookVarsMap someVarsMap)
          throws ZetaMagicHandleException, ZetaVariableInjectionException {

    if (!enabled) {
      LOGGER.info("The dynamic variable is not enabled");
      return notHandled(interpreter);
    }

    if (someVarsMap != null) {
      // try match magic
      /** the comment pos should align with code, if there is no comment in code, the comment should be null or empty**/
      MagicPattern.MatcherAndPattern matcherAndPattern = MagicPattern.tryMatch(codeWithSeq.getCode(), codeWithSeq.getCommentPos());
      if (matcherAndPattern.isMatched()) {
        LOGGER.info("Handling zeta magic statement: {} for note: {}, nt: {}",
                matcherAndPattern.pattern.name(), notebook.getId(), notebook.getNt());
        // if handled it will return a not null VariableHandlerCallback instance
        // otherwise it will throw exception
        PatternHandler patternHandler = matcherAndPattern.pattern.getHandler();
        return patternHandler.handle(new HandleContext(interpreter, matcherAndPattern.matcher, codeWithSeq, notebook, someVarsMap, repository));
      }

      // try match variables
      LOGGER.info("Injecting variables for statement: {}, note: {}, nt: {}",
              codeWithSeq.getStatementId(), notebook.getId(), notebook.getNt());
      try {
        DynamicReplaceMatcher drMatcher = DynamicReplaceMatcher.matchAllAndReplace(
                codeWithSeq.getCode(),
                (String matchedVar) -> matchAndReplaceVars(matchedVar, someVarsMap),
                /** the comment pos should align with code, if there is no comment in code, the comment should be null or empty**/
                codeWithSeq.getCommentPos(),
                PATTERN_A, PATTERN_B);
        if (drMatcher.replaced) {
          codeWithSeq.setCode(drMatcher.result);
          LOGGER.info("Code replaced to: {}", drMatcher.result);
          return new HandleStateContext(HandleState.INJECTED, ExecutionAgent.forwardAgent(interpreter));
        }
      } catch (Exception ex) {
        throw ex instanceof NullPointerException ?
                new ZetaVariableInjectionException(ExceptionUtils.getFullStackTrace(ex))
                : new ZetaVariableInjectionException(ex);
      }
    } else {
      LOGGER.info("Can't find any vars in note: {}, nt: {}, skip vars handling", notebook.getId(), notebook.getNt());
    }

    return notHandled(interpreter);
  }

  public static String matchAndReplaceVars(String nameWithClause, NotebookVarsMap varsMap) {
    // only check the head and tail since the pattern already matched
    String varName = NotebookVariable.getTrimmedVariableName(nameWithClause);
    String value = "";
    NotebookVariable variable = varsMap.get(varName);
    if (variable != null && variable.value != null) {
      value = variable.value;
      LOGGER.info("Variable: {} injected to: {}", varName, value);
    } else {
      LOGGER.info("Can't find any value for variable: {}, leave it to empty string", varName);
    }
    return value;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }
}
