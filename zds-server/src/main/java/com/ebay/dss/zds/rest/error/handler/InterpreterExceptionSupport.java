package com.ebay.dss.zds.rest.error.handler;

import com.ebay.dss.zds.exception.ErrorCode;
import com.ebay.dss.zds.exception.InterpreterExecutionException;
import com.ebay.dss.zds.exception.InterpreterServiceException;
import com.ebay.dss.zds.rest.error.match.ExceptionRuleService;
import com.ebay.dss.zds.model.ZetaStatus;
import com.ebay.dss.zds.rest.error.ErrorDTO;
import com.ebay.dss.zds.rest.error.SimpleCauseError;
import com.ebay.dss.zds.websocket.WebSocketResp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InterpreterExceptionSupport extends ExceptionHandlerSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(InterpreterExceptionSupport.class);

    public InterpreterExceptionSupport(ExceptionRuleService ruleService) {
        super(ruleService);
    }

    public WebSocketResp<ErrorDTO> handleInterpreterConnectionException(InterpreterServiceException ise) {
        LOGGER.info("Get interpreter connection exception");
        WebSocketResp.OP op = WebSocketResp.OP.INTERNAL_ERROR;
        if (ErrorCode.INTERPRETER_CONNECT_EXCEPTION == ise.getErrorCode()) {
            op = WebSocketResp.OP.CONNECTION_ERROR;
            if (ZetaStatus.INTP_SESSION_CONNECTION_ABORT == ise.getZetaStatus()) {
                op = WebSocketResp.OP.CONNECTION_ABORT;
            }
        } else if (ErrorCode.INTERPRETER_DISCONNECT_EXCEPTION == ise.getErrorCode()) {
            op = WebSocketResp.OP.DISCONNECTION_ERROR;
        }

        ErrorDTO error = ruleMatch(ise, () -> new ErrorDTO(ise.getErrorCode(), SimpleCauseError.from(ise)));
        return WebSocketResp.get(op, error);
    }

    public WebSocketResp<ErrorDTO> handleInterpreterExecutionException(InterpreterExecutionException iee) {
        LOGGER.info("Get interpreter execution exception");
        WebSocketResp.OP op = WebSocketResp.OP.INTERNAL_ERROR;
        if (ErrorCode.INTERPRETER_SESSION_EXPIRED_EXCEPTION == iee.getErrorCode()) {
            op = WebSocketResp.OP.NB_CODE_SESSION_EXPIRED;
        } else if (ErrorCode.INTERPRETER_INVALID_NOTEBOOK_STATUS_EXCEPTION == iee.getErrorCode()) {
            op = WebSocketResp.OP.NB_CODE_INVALID_NOTEBOOK_STATUS;
        } else if (ErrorCode.INTERPRETER_CODE_EXECUTE_EXCEPTION == iee.getErrorCode()) {
            op = WebSocketResp.OP.NB_CODE_EXECUTE_ERROR;
        } else if (ErrorCode.INTERPRETER_CODE_EXECUTE_CANCEL_EXCEPTION == iee.getErrorCode()) {
            op = WebSocketResp.OP.CANCEL_ERROR;
        }

        ErrorDTO error = ruleMatch(iee, () -> new ErrorDTO(iee.getErrorCode(), SimpleCauseError.from(iee)));
        return WebSocketResp.get(op, error);
    }
}
