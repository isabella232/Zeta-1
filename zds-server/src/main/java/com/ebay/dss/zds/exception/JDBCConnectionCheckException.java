package com.ebay.dss.zds.exception;

/**
 * Created by tatian on 2019-05-14.
 */
public class JDBCConnectionCheckException extends ApplicationBaseException{
    public static final String defaultMessage = "JDBC connection check failed";
    public JDBCConnectionCheckException(String message) {
        super(ErrorCode.JDBC_CONNECTION_CHECK_EXCEPTION, message);
    }

    public JDBCConnectionCheckException(String message, Throwable t) {
        super(ErrorCode.JDBC_CONNECTION_CHECK_EXCEPTION, message, t);
    }
}
