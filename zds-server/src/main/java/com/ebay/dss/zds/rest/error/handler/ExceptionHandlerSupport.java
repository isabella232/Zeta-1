package com.ebay.dss.zds.rest.error.handler;

import com.ebay.dss.zds.exception.ApplicationBaseException;
import com.ebay.dss.zds.exception.ErrorCode;
import com.ebay.dss.zds.model.ExceptionRule;
import com.ebay.dss.zds.rest.error.match.ExceptionRuleService;
import com.ebay.dss.zds.rest.error.match.RuleMatchResult;
import com.ebay.dss.zds.model.ZetaStatus;
import com.ebay.dss.zds.rest.error.*;
import com.ebay.dss.zds.websocket.WebSocketResp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.function.Supplier;

public abstract class ExceptionHandlerSupport {

    private final static Logger logger = LoggerFactory.getLogger(ExceptionHandlerSupport.class);

    private final ExceptionRuleService ruleService;

    public ExceptionHandlerSupport(ExceptionRuleService ruleService) {
        this.ruleService = ruleService;
    }

    private static ErrorDTO formatErrorDto(ErrorDTO base, RuleMatchResult matchResult) {
        ErrorDTO result = adjustCode(base, matchResult);
        result = adjustDetail(base, matchResult);
        return result;
    }

    private static ErrorDTO adjustCode(ErrorDTO base, RuleMatchResult matchResult) {
        ExceptionRule matchedRule = matchResult.getMatchedRule();
        if (!matchedRule.isMessageOnly()) {
            String errorCode = matchedRule.getErrorCode();
            ErrorCode code = ErrorCode.from(errorCode);
            base.setCode(code.name());
        }
        return base;
    }

    private static ErrorDTO adjustDetail(ErrorDTO base, RuleMatchResult matchResult) {
        String message = matchResult.getFormattedMessage();
        BaseErrorDetail detail = base.getErrorDetail();
        if (detail instanceof StringMessageError) {
            StringMessageError messageError = (StringMessageError) detail;
            String originalMessage = messageError.getMessage();
            messageError.setMessage(message);
            messageError.setOriginalMessage(originalMessage);
        }
        detail.setRule(matchResult.getMatchedRule());
        base.setErrorDetail(detail);
        return base;
    }

    public ErrorDTO ruleMatch(Throwable t, Supplier<ErrorDTO> baseSupplier) {
        logger.error(t.getMessage(), t);
        RuleMatchResult matchResult = ruleService.match(t);
        ErrorDTO errorDTO = baseSupplier.get();
        if (matchResult.isMatched()) {
            errorDTO = formatErrorDto(errorDTO, matchResult);
        }

        return errorDTO;
    }

    @Deprecated
    public ResponseEntity<ErrorDTO> ruleMatch(Throwable t, ZetaStatus zetaStatus, HttpStatus status) {
        ErrorDTO error = ruleMatch(t, () -> new ErrorDTO(zetaStatus.getStatusCode(), StringMessageError.from(t.getMessage())));
        return ResponseEntity.status(status).body(error);
    }

    public ResponseEntity<ErrorDTO> ruleMatch(Throwable t, ErrorCode errorCode) {
        ErrorDTO error = ruleMatch(t, () -> new ErrorDTO(errorCode, StringMessageError.from(t.getMessage())));
        if (errorCode instanceof ErrorCode.WithHttpStatus) {
            return ResponseEntity.status(((ErrorCode.WithHttpStatus) errorCode).status()).body(error);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    public ResponseEntity<ErrorDTO> ruleMatch(ApplicationBaseException t, HttpStatus status) {
        ErrorDTO error = ruleMatch(t, () -> new ErrorDTO(t.getErrorCode(), StringMessageError.from(t.getMessage())));
        return ResponseEntity.status(status).body(error);
    }

    public WebSocketResp<ErrorDTO> ruleMatch(Throwable t, ErrorCode errorCode, WebSocketResp.OP op) {
        ErrorDTO error = ruleMatch(t, () -> new ErrorDTO(errorCode, StringMessageError.from(t.getMessage())));
        return WebSocketResp.get(op, error);
    }

}
