package com.ebay.dss.zds.rest.error.handler;

import com.ebay.dss.zds.common.ExceptionUtil;
import com.ebay.dss.zds.exception.*;
import com.ebay.dss.zds.exception.ErrorCode;
import com.ebay.dss.zds.rest.error.match.ExceptionRuleService;
import com.ebay.dss.zds.model.ZetaStatus;
import com.ebay.dss.zds.rest.error.ConstraintViolationsError;
import com.ebay.dss.zds.rest.error.ErrorDTO;
import com.ebay.dss.zds.rest.error.StringMessageError;
import io.micrometer.core.lang.Nullable;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.IncorrectResultSetColumnCountException;
import org.springframework.jdbc.JdbcUpdateAffectedIncorrectNumberOfRowsException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.ConstraintViolationException;
import java.io.IOException;

/**
 * Created by wenliu2 on 4/3/18.
 */
@ControllerAdvice(annotations = {RestController.class, Controller.class})
public class RestResponseEntityExceptionHandler extends ExceptionHandlerSupport {

    public RestResponseEntityExceptionHandler(ExceptionRuleService ruleService) {
        super(ruleService);
    }

    @ExceptionHandler(value = Throwable.class)
    public ResponseEntity<ErrorDTO> handleOthersException(Throwable t) {
        return ruleMatch(t, ErrorCode.UNKNOWN_EXCEPTION);
    }


    @ExceptionHandler(value = {ApplicationBaseException.class})
    @Nullable
    public ResponseEntity<ErrorDTO> handleBadRequest(ApplicationBaseException ex) {

        // execute call back
        ex.getExceptionListener().onExceptionCaught();

        //String bodyOfResponse = "This should be application specific";
        if (ex instanceof DuplicationException || ex instanceof ToolSetCheckException) {
            return ruleMatch(ex, HttpStatus.BAD_REQUEST);
        } else if (ex instanceof PermissionDenyException) {
            return ruleMatch(ex, HttpStatus.UNAUTHORIZED);
        } else {
            ExceptionUtil.cleanStackTrace(ex.getCause());
            ErrorCode code = ex.getErrorCode();
            return ruleMatch(ex, code);
        }
    }

    @ExceptionHandler(value = DataIntegrityViolationException.class)
    public ResponseEntity<ErrorDTO> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        Throwable t;
        if (ex.getCause() instanceof org.hibernate.exception.ConstraintViolationException) {
            t = ((org.hibernate.exception.ConstraintViolationException) ex.getCause()).getSQLException();
        } else {
            t = ex;
        }
        return ruleMatch(t, ZetaStatus.CONFLICT_EXCEPTION, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(value = BadSqlGrammarException.class)
    public ResponseEntity<ErrorDTO> handleBadSqlGrammarException(BadSqlGrammarException ex) {
        return ruleMatch(ex, ZetaStatus.BAD_SQL_GRAMMER, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = CannotGetJdbcConnectionException.class)
    public ResponseEntity<ErrorDTO> handleCannotGetJdbcConnectionException(CannotGetJdbcConnectionException ex) {
        return ruleMatch(ex, ZetaStatus.NO_JDBC_CONNECTION, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(value = JdbcUpdateAffectedIncorrectNumberOfRowsException.class)
    public ResponseEntity<ErrorDTO> handleJdbcUpdateAffectedIncorrectNumberOfRowsException(JdbcUpdateAffectedIncorrectNumberOfRowsException ex) {
        return ruleMatch(ex, ZetaStatus.ILLEGAL_STATUS, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = IncorrectResultSetColumnCountException.class)
    public ResponseEntity<ErrorDTO> handleIncorrectResultSetColumnCountException(IncorrectResultSetColumnCountException ex) {
        return ruleMatch(ex, ZetaStatus.INCORRECT_SET_COLUMN_COUNT, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = DuplicateKeyException.class)
    public ResponseEntity<ErrorDTO> handleDuplicateKeyException(DuplicateKeyException ex) {
        return ruleMatch(ex, ZetaStatus.DUPLICATION_VIOLATION, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = QueryTimeoutException.class)
    public ResponseEntity<ErrorDTO> handleQueryTimeoutException(QueryTimeoutException ex) {
        return ruleMatch(ex, ZetaStatus.TIMEOUT, HttpStatus.REQUEST_TIMEOUT);
    }

    @ExceptionHandler(value = IOException.class)
    public ResponseEntity<ErrorDTO> handleIOException(IOException ex) {
        return ruleMatch(ex, ZetaStatus.FAIL, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = TypeMismatchException.class)
    public ResponseEntity<ErrorDTO> handleTypeMismatchException(TypeMismatchException ex) {
        return ruleMatch(ex, ZetaStatus.FAIL, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = ConstraintViolationException.class)
    public ResponseEntity<ErrorDTO> handlerConstraintViolation(ConstraintViolationException ex) {
        ErrorDTO error = ruleMatch(ex, () -> new ErrorDTO(ErrorCode.INVALID_INPUT, ConstraintViolationsError.from(ex.getConstraintViolations())));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(value = EntityNotFoundException.class)
    public ResponseEntity<ErrorDTO> handleEntityNotFoundException(EntityNotFoundException ex) {
        return ruleMatch(ex, ErrorCode.ENTITY_IS_NULL);
    }

    @ExceptionHandler(value = InterpreterStoppedException.class)
    public ResponseEntity<ErrorDTO> handleInterpreterStoppedException(InterpreterStoppedException ex) {
        // this exception will happened with the notebook's disconnection operation when:
        // 1. cancel connection
        // 2. disconnect a connected notebook
        // so this ex will be an exception that will happened during a notebook's life cycle and considered as normal event
        // thus, the status should be OK. will change in the future if there are other concerns
        ErrorDTO error = ruleMatch(ex, () -> new ErrorDTO(ErrorCode.INTERPRETER_STOPPED_EXCEPTION, StringMessageError.from(ex)));
        // execute call back
        // here, will clean up the interpreter's context
        ex.getExceptionListener().onExceptionCaught();
        return ResponseEntity.status(HttpStatus.OK).body(error);
    }

}
