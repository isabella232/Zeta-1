package com.ebay.dss.zds.interpreter.interpreters.jdbc;

public class Constant {

    public static final String DEFAULT_TEST_SQL = "select 1";
    public static final String MAX_ROWS_KEY = "jdbc.rows.max.limit";
    public static final String DEFAULT_MAX_ROWS = "1000";
    public static final int DEFAULT_MAX_POOL_SIZES = 1;
    public static final String JDBC_CONF_KEY = "post_processor";
    public static final String JDBC_TYPE_KEY = "jdbc_type";
    public static final String HOST_KEY = "host";
    public static final String PORT_KEY = "port";
    public static final String USER_KEY = "user";
    public static final String PASSWORD_KEY = "password";
    public static final String DATABASE_KEY = "database";
    public static final String SSL_KEY = "ssl";
    public static final String JDBC_URL_PARAMS_PREFIX_REGEX = "^jdbc\\.url\\.params\\.";
    public static final String JDBC_URL_PARAMS_REGEX = JDBC_URL_PARAMS_PREFIX_REGEX + "(.*)$";
    public static final String JDBC_URL_CARMEL_PARAMS_PREFIX_REGEX = "^jdbc\\.url\\.external\\.params\\.";
    public static final String JDBC_URL_CARMEL_PARAMS_REGEX = JDBC_URL_CARMEL_PARAMS_PREFIX_REGEX + "(.*)$";
    public static final String JDBC_PROPS_PREFIX_REGEX = "^jdbc\\.props\\.";
    public static final String JDBC_PROPS_REGEX = JDBC_PROPS_PREFIX_REGEX + "(.*)$";
    public static final String DEFAULT_DATABASE_PARAMS_SEPARATOR = "?";
    public static final String DEFAULT_PARAMS_DELIMITER = ";";
    public static final String CARMEL_PROPERTIES_SEPARATOR = "?";
    public static final String CARMEL_PROPERTIES_DELIMITER = ";";
}
