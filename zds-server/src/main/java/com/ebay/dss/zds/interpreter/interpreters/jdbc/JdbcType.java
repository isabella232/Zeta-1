package com.ebay.dss.zds.interpreter.interpreters.jdbc;

import com.ebay.dss.zds.interpreter.InterpreterType;

import static com.ebay.dss.zds.interpreter.interpreters.jdbc.Constant.*;
import static com.ebay.dss.zds.interpreter.interpreters.jdbc.Constant.DEFAULT_TEST_SQL;

public enum JdbcType {
    teradata("teradata", "com.teradata.jdbc.TeraDriver",
            "", ",",
            InterpreterType.EnumType.JDBC,
            "--valid", true, false),
    kylin("kylin", "org.apache.kylin.jdbc.Driver",
            DEFAULT_DATABASE_PARAMS_SEPARATOR, DEFAULT_PARAMS_DELIMITER,
            InterpreterType.EnumType.JDBC,
            DEFAULT_TEST_SQL, true, false),
    hive("hive2", "org.apache.hive.jdbc.HiveDriver",
            ";", DEFAULT_PARAMS_DELIMITER,
            InterpreterType.EnumType.JDBC,
            DEFAULT_TEST_SQL, true, false);


    public final String protocol;
    public final String driverClassName;
    public final String databaseParamsSeparator;
    public final String paramsDelimiter;
    public final String testSQL;
    public final boolean testOnConnection;
    public final boolean testPeriodically;
    public final InterpreterType.EnumType interpreterType;

    JdbcType(String protocol,
             String driverClassName,
             String databaseParamsSeparator,
             String paramsDelimiter,
             InterpreterType.EnumType interpreterType,
             String testSQL,
             boolean testAfterConnection,
             boolean testPeriodically) {
        this.protocol = protocol;
        this.driverClassName = driverClassName;
        this.databaseParamsSeparator = databaseParamsSeparator;
        this.paramsDelimiter = paramsDelimiter;
        this.interpreterType = interpreterType;
        this.testSQL = testSQL;
        this.testOnConnection = testAfterConnection;
        this.testPeriodically = testPeriodically;
    }

    public String fullProtocol() {
        return "jdbc:" + protocol;
    }
}

