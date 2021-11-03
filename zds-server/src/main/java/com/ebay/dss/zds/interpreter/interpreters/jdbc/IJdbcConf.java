package com.ebay.dss.zds.interpreter.interpreters.jdbc;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;

public interface IJdbcConf {

    String getId();

    IJdbcConf setId(String id);

    Callable<DataSource> getDataSourceSupplier();

    IJdbcConf setDataSourceSupplier(Callable<DataSource> callableDataSourceSupplier);

    String getNotebookId();

    IJdbcConf setNotebookId(String id);

    int getPort();

    IJdbcConf setPort(int port);

    boolean isSsl();

    IJdbcConf setSsl(boolean ssl);

    String getNt();

    IJdbcConf setNt(String nt);

    Properties getProps();

    IJdbcConf setProps(Properties props);

    String url();

    Object get(Object key);

    Object put(Object key, Object value);

    String getProperty(String key);

    String getProperty(String key, String defaultValue);

    String putUrlParam(String key, String value);

    String putProperty(String key, String value);

    JdbcType getJdbcType();

    IJdbcConf setJdbcType(JdbcType jdbcType);

    String getHost();

    IJdbcConf setHost(String host);

    String getUser();

    IJdbcConf setUser(String user);

    String getPassword();

    IJdbcConf setPassword(String password);

    String getDatabase();

    IJdbcConf setDatabase(String database);

    Map<String, String> getUrlParams();

    IJdbcConf setUrlParams(Map<String, String> urlParams);

    int getRowMax();

    IJdbcConf setRowMax(int limit);
}
