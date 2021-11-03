package com.ebay.dss.zds.rest.error.handler;

import com.ebay.dss.zds.exception.ErrorCode;
import com.ebay.dss.zds.rest.error.match.ExceptionRuleService;
import com.ebay.dss.zds.rest.KylinResourceController;
import com.ebay.dss.zds.rest.error.ErrorDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

@ControllerAdvice(assignableTypes = KylinResourceController.class)
public class KylinReadableExceptionHandler extends ExceptionHandlerSupport {

    public KylinReadableExceptionHandler(ExceptionRuleService ruleService) {
        super(ruleService);
    }

    @ExceptionHandler(value = HttpClientErrorException.class)
    public ResponseEntity<ErrorDTO> handleRestClientException(HttpClientErrorException ex) {
        return ruleMatch(ex, ErrorCode.KYLIN_API_UNAVAILABLE);
    }

    @ExceptionHandler(value = ResourceAccessException.class)
    public ResponseEntity<ErrorDTO> handleRestClientException(ResourceAccessException ex) {
        return ruleMatch(ex, ErrorCode.NOT_FOUND);
    }
}
