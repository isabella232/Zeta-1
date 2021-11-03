package com.ebay.dss.zds.exception;

import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by wenliu2 on 4/3/18.
 */
public interface ErrorCode {
  
    Map<String, ErrorCode> NAME_MAP = new HashMap<>();
    ErrorCode MALFORMED_INFORMATION = WithHttpStatus.builtInErrorCode("MALFORMED_INFORMATION", HttpStatus.INTERNAL_SERVER_ERROR);
    ErrorCode DUPLICATION = WithHttpStatus.builtInErrorCode("DUPLICATION", HttpStatus.CONFLICT);
    ErrorCode INVALID_INPUT = WithHttpStatus.builtInErrorCode("INVALID_INPUT", HttpStatus.BAD_REQUEST);
    ErrorCode ENTITY_IS_NULL = WithHttpStatus.builtInErrorCode("ENTITY_IS_NULL", HttpStatus.NOT_FOUND);
    ErrorCode ENTITY_ID_EMPTY = WithHttpStatus.builtInErrorCode("ENTITY_ID_EMPTY", HttpStatus.BAD_REQUEST);
    ErrorCode ILLEGAL_STATUS = WithHttpStatus.builtInErrorCode("ILLEGAL_STATUS", HttpStatus.BAD_REQUEST);
    ErrorCode TOOLSET_EXCEPTION = WithHttpStatus.builtInErrorCode("TOOLSET_EXCEPTION", HttpStatus.BAD_REQUEST);
    ErrorCode NOTEBOOK_EXCEPTION = WithHttpStatus.builtInErrorCode("NOTEBOOK_EXCEPTION", HttpStatus.BAD_REQUEST);
    ErrorCode INVALID_NT = WithHttpStatus.builtInErrorCode("INVALID_NT", HttpStatus.BAD_REQUEST);
    ErrorCode SCHEDULE_EXCEPTION = WithHttpStatus.builtInErrorCode("SCHEDULE_EXCEPTION", HttpStatus.INTERNAL_SERVER_ERROR);
    ErrorCode GIT_EXCEPTION = WithHttpStatus.builtInErrorCode("GIT_EXCEPTION", HttpStatus.INTERNAL_SERVER_ERROR);
    ErrorCode FILESTORAGE_EXCEPTIOM = WithHttpStatus.builtInErrorCode("FILESTORAGE_EXCEPTIOM", HttpStatus.INTERNAL_SERVER_ERROR);
    ErrorCode NOT_FOUND = WithHttpStatus.builtInErrorCode("NOT_FOUND", HttpStatus.NOT_FOUND);
    ErrorCode UNAUTHORIZED = WithHttpStatus.builtInErrorCode("UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
    ErrorCode UNKNOWN_EXCEPTION = WithHttpStatus.builtInErrorCode("UNKNOWN_EXCEPTION", HttpStatus.INTERNAL_SERVER_ERROR);
    ErrorCode METATABLE_EXCEPTION = WithHttpStatus.builtInErrorCode("METATABLE_EXCEPTION", HttpStatus.BAD_REQUEST);
    ErrorCode CLUSTER_ACCESS_DENIED_EXCEPTION = WithHttpStatus.builtInErrorCode("CLUSTER_ACCESS_DENIED_EXCEPTION", HttpStatus.BAD_REQUEST);
    ErrorCode AUTHORIZATION_EXCEPTION = WithHttpStatus.builtInErrorCode("AUTHORIZATION_EXCEPTION", HttpStatus.INTERNAL_SERVER_ERROR);
    //configuration
    ErrorCode CONFIGURATION_EXCEPTION = WithHttpStatus.builtInErrorCode("CONFIGURATION_EXCEPTION", HttpStatus.BAD_REQUEST);
    //interpreter
    ErrorCode INTERPRETER_GENERIC_EXCEPTION = WithHttpStatus.builtInErrorCode("INTERPRETER_GENERIC_EXCEPTION", HttpStatus.BAD_REQUEST);
    ErrorCode INTERPRETER_STOPPED_EXCEPTION = WithHttpStatus.builtInErrorCode("INTERPRETER_STOPPED_EXCEPTION", HttpStatus.BAD_REQUEST);
    ErrorCode INTERPRETER_INVALID_TYPE_EXCEPTION = WithHttpStatus.builtInErrorCode("INTERPRETER_INVALID_TYPE_EXCEPTION", HttpStatus.BAD_REQUEST);
    ErrorCode INTERPRETER_SERVICE_EXCEPTION = WithHttpStatus.builtInErrorCode("INTERPRETER_SERVICE_EXCEPTION", HttpStatus.INTERNAL_SERVER_ERROR);
    ErrorCode INTERPRETER_CONNECT_EXCEPTION = WithHttpStatus.builtInErrorCode("INTERPRETER_CONNECT_EXCEPTION", HttpStatus.INTERNAL_SERVER_ERROR);
    ErrorCode INTERPRETER_DISCONNECT_EXCEPTION = WithHttpStatus.builtInErrorCode("INTERPRETER_DISCONNECT_EXCEPTION", HttpStatus.INTERNAL_SERVER_ERROR);
    ErrorCode INTERPRETER_SESSION_EXPIRED_EXCEPTION = WithHttpStatus.builtInErrorCode("INTERPRETER_SESSION_EXPIRED_EXCEPTION", HttpStatus.INTERNAL_SERVER_ERROR);
    ErrorCode INTERPRETER_INVALID_NOTEBOOK_STATUS_EXCEPTION = WithHttpStatus.builtInErrorCode("INTERPRETER_INVALID_NOTEBOOK_STATUS_EXCEPTION", HttpStatus.INTERNAL_SERVER_ERROR);
    ErrorCode INTERPRETER_CODE_EXECUTE_EXCEPTION = WithHttpStatus.builtInErrorCode("INTERPRETER_CODE_EXECUTE_EXCEPTION", HttpStatus.INTERNAL_SERVER_ERROR);
    ErrorCode INTERPRETER_CODE_EXECUTE_CANCEL_EXCEPTION = WithHttpStatus.builtInErrorCode("INTERPRETER_CODE_EXECUTE_CANCEL_EXCEPTION", HttpStatus.INTERNAL_SERVER_ERROR);
    //jdbc
    ErrorCode JDBC_CONNECTION_CHECK_EXCEPTION = WithHttpStatus.builtInErrorCode("JDBC_CONNECTION_CHECK_EXCEPTION", HttpStatus.INTERNAL_SERVER_ERROR);
    ErrorCode JDBC_CONNECTION_CLOSED_EXCEPTION = WithHttpStatus.builtInErrorCode("JDBC_CONNECTION_CLOSED_EXCEPTION", HttpStatus.INTERNAL_SERVER_ERROR);
    //lifecycle
    ErrorCode LIFECYCLE_EXCEPTION = WithHttpStatus.builtInErrorCode("LIFECYCLE_EXCEPTION", HttpStatus.BAD_REQUEST);
    //kylin api
    ErrorCode KYLIN_API_UNAVAILABLE = WithHttpStatus.builtInErrorCode("KYLIN_API_UNAVAILABLE", HttpStatus.INTERNAL_SERVER_ERROR);
    ErrorCode EXIST_NOTEBOOK = WithHttpStatus.builtInErrorCode("EXIST_NOTEBOOK", HttpStatus.CONFLICT);
    ErrorCode Cannot_find_Query = WithHttpStatus.builtInErrorCode("Cannot find Query", HttpStatus.NOT_FOUND);
    ErrorCode UNKNOWN_HIVE_REALM = WithHttpStatus.builtInErrorCode("Realm of hive sever is not recognized", HttpStatus.NOT_FOUND);
    //zeta sheet
    ErrorCode SHEET_SYNC_LOAD_FAIL = WithHttpStatus.builtInErrorCode("SHEET_SYNC_LOAD_FAIL", HttpStatus.INTERNAL_SERVER_ERROR);
    ErrorCode SHEET_SYNC_REGISTER_FAIL = WithHttpStatus.builtInErrorCode("SHEET_SYNC_REGISTER_FAIL", HttpStatus.INTERNAL_SERVER_ERROR);


    static ErrorCode from(String name) {
        return NAME_MAP.getOrDefault(name, UNKNOWN_EXCEPTION);
    }

    static ErrorCode fromUnsafe(String name) {
        return NAME_MAP.get(name);
    }

    String name();

    class WithHttpStatus implements ErrorCode {

        private final String name;
        private final HttpStatus status;

        WithHttpStatus(String name, HttpStatus status) {
            this.name = name;
            this.status = status;
        }

        private static WithHttpStatus builtInErrorCode(String name, HttpStatus status) {
            WithHttpStatus code = of(name, status);
            NAME_MAP.put(code.name(), code);
            return code;
        }

        public static WithHttpStatus of(String name, HttpStatus status) {
            return new WithHttpStatus(name, status);
        }

        public HttpStatus status() {
            return status;
        }

        @Override
        public String name() {
            return name;
        }
    }
}
