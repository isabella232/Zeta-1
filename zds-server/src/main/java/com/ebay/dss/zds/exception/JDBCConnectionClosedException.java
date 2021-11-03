package com.ebay.dss.zds.exception;

/**
 * Created by tatian on 2019-05-14.
 */
public class JDBCConnectionClosedException extends ApplicationBaseException{
    public static final String defaultMessage = "JDBC connection is closed";
    public JDBCConnectionClosedException(String message) {
        super(ErrorCode.JDBC_CONNECTION_CLOSED_EXCEPTION, message);
    }
    public JDBCConnectionClosedException() {
        super(ErrorCode.JDBC_CONNECTION_CLOSED_EXCEPTION, defaultMessage);
    }
}
