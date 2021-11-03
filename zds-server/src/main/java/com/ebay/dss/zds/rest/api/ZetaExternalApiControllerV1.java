package com.ebay.dss.zds.rest.api;

import com.ebay.dss.zds.common.ExceptionUtil;
import com.ebay.dss.zds.exception.InterpreterSessionNotFoundException;
import com.ebay.dss.zds.interpreter.api.InterpreterService;
import com.ebay.dss.zds.interpreter.api.InterpreterSession;
import com.ebay.dss.zds.interpreter.api.InterpreterStatement;
import com.ebay.dss.zds.interpreter.api.dto.ExecuteCodeRequest;
import com.ebay.dss.zds.interpreter.api.dto.InterpreterSessionRequest;
import com.ebay.dss.zds.interpreter.input.ExecutionContext;
import com.ebay.dss.zds.model.ZetaResponse;
import com.ebay.dss.zds.service.api.ZetaExternalApiService;
import org.codehaus.plexus.util.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Created by tatian on 2021/3/30.
 */

@RestController
@RequestMapping("/api/v1")
public class ZetaExternalApiControllerV1 {

  private static final Logger logger = LoggerFactory.getLogger(ZetaExternalApiControllerV1.class);

  private final ZetaExternalApiService apiService;

  @Autowired
  public ZetaExternalApiControllerV1(ZetaExternalApiService apiService) {
    this.apiService = apiService;
  }

  @RequestMapping(path = "/interpreter/session/open", method = RequestMethod.POST)
  @ResponseBody
  public ZetaResponse openInterpreterSession(@RequestBody InterpreterSessionRequest request) {
    try {
      InterpreterSession interpreterSession = apiService.tryOpenInterpreterSessionAsync(request);
      return new ZetaResponse<>(interpreterSession.toJson(), HttpStatus.OK);
    } catch (Exception ex) {
      logger.error(ExceptionUtils.getFullStackTrace(ex));
      return new ZetaResponse<>(ex.toString(), HttpStatus.BAD_REQUEST);
    }
  }

  @RequestMapping(path = "/interpreter/session/{id}", method = RequestMethod.GET)
  @ResponseBody
  public ZetaResponse getInterpreterSession(@PathVariable("id") String id) {
    try {
      InterpreterSession interpreterSession = apiService.getInterpreterSession(id);
      return new ZetaResponse<>(interpreterSession.toJson(), HttpStatus.OK);
    } catch (Exception ex) {
      logger.error(ExceptionUtils.getFullStackTrace(ex));
      return new ZetaResponse<>(ex.toString(), HttpStatus.NOT_FOUND);
    }
  }

  @RequestMapping(path = "/interpreter/session/{id}", method = RequestMethod.DELETE)
  @ResponseBody
  public ZetaResponse deleteInterpreterSession(@PathVariable("id") String id) {
    try {
      apiService.closeInterpreterSession(id);
    } catch (Exception ex) {
      logger.error(ExceptionUtils.getFullStackTrace(ex));
    }
    return new ZetaResponse<>("Deleted", HttpStatus.OK);
  }

  @RequestMapping(path = "/interpreter/execute", method = RequestMethod.POST)
  @ResponseBody
  public ZetaResponse executeCode(@RequestBody ExecuteCodeRequest request) {
    try {
      InterpreterStatement statement = apiService.tryExecuteStatementAsync(request);
      return new ZetaResponse<>(statement.toJson(), HttpStatus.OK);
    } catch (InterpreterSessionNotFoundException isnf) {
      logger.error(isnf.toString());
      return new ZetaResponse<>(isnf.toString(), HttpStatus.NOT_FOUND);
    } catch (Exception ex) {
      logger.error(ExceptionUtils.getFullStackTrace(ex));
      return new ZetaResponse<>(ex.toString(), HttpStatus.BAD_REQUEST);
    }
  }

  @RequestMapping(path = "/interpreter/session/{sessionId}/statement/{statementId}", method = RequestMethod.GET)
  @ResponseBody
  public ZetaResponse getInterpreterStatement(@PathVariable("sessionId") String sessionId,
                                              @PathVariable("statementId") Long statementId) {
    try {
      InterpreterStatement statement = apiService.getStatement(sessionId, statementId);
      return new ZetaResponse<>(statement.toJson(), HttpStatus.OK);
    } catch (Exception ex) {
      logger.error(ExceptionUtils.getFullStackTrace(ex));
      return new ZetaResponse<>(ex.toString(), HttpStatus.NOT_FOUND);
    }
  }

  @RequestMapping(path = "/interpreter/session/{sessionId}/statement/{statementId}", method = RequestMethod.DELETE)
  @ResponseBody
  public ZetaResponse cancelInterpreterStatement(@PathVariable("sessionId") String sessionId,
                                                 @PathVariable("statementId") Long statementId) {
    try {
      apiService.cancelStatement(sessionId, statementId);
    } catch (Exception ex) {
      logger.error(ExceptionUtils.getFullStackTrace(ex));
    }
    return new ZetaResponse<>("Canceled", HttpStatus.OK);
  }
}
