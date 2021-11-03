package com.ebay.dss.zds.rest.error.handler;

import com.ebay.dss.zds.exception.InterpreterExecutionException;
import com.ebay.dss.zds.exception.InterpreterServiceException;
import com.ebay.dss.zds.rest.error.match.ExceptionRuleService;
import com.ebay.dss.zds.rest.ExceptionRuleController;
import com.ebay.dss.zds.rest.error.ErrorDTO;
import com.ebay.dss.zds.websocket.WebSocketResp;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * This is for test interpreter exception;
 */
@ControllerAdvice(assignableTypes = ExceptionRuleController.class)
public class InterpreterExceptionHandler extends InterpreterExceptionSupport {

    public InterpreterExceptionHandler(ExceptionRuleService ruleService) {
        super(ruleService);
    }

    @ExceptionHandler(value = {InterpreterServiceException.class})
    public ResponseEntity<WebSocketResp<ErrorDTO>> handleInterpreterConnectionExceptionRest(InterpreterServiceException ise) {
        WebSocketResp<ErrorDTO> resp = handleInterpreterConnectionException(ise);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
    }

    @ExceptionHandler(value = {InterpreterExecutionException.class})
    public ResponseEntity<WebSocketResp<ErrorDTO>> handleInterpreterExecutionExceptionRest(InterpreterExecutionException iee) {
        WebSocketResp<ErrorDTO> resp = handleInterpreterExecutionException(iee);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
    }
}
