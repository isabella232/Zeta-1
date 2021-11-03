package com.ebay.dss.zds.interpreter.interpreters.jdbc;

import org.apache.commons.lang3.StringUtils;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public class JdbcConf implements IJdbcConf {

    private String id;
    private String nt;
    private String notebookId;
    private JdbcType jdbcType;
    private String host;
    private String user;
    private String password;
    private String database;
    private int port;
    private boolean ssl;
    private Map<String, String> urlParams;
    private Properties props;
    private Callable<DataSource> dataSourceSupplier;
    private int rowMax;


    public String getId() {
        return id;
    }

    public IJdbcConf setId(String id) {
        this.id = id;
        return this;
    }

    public Callable<DataSource> getDataSourceSupplier() {
        return dataSourceSupplier;
    }

    public JdbcConf setDataSourceSupplier(Callable<DataSource> dataSourceSupplier) {
        this.dataSourceSupplier = dataSourceSupplier;
        return this;
    }

    public String getNotebookId() {
        return notebookId;
    }

    public JdbcConf setNotebookId(String notebookId) {
        this.notebookId = notebookId;
        return this;
    }

    public int getPort() {
        return port;
    }

    public JdbcConf setPort(int port) {
        this.port = port;
        return this;
    }

    public boolean isSsl() {
        return ssl;
    }

    public JdbcConf setSsl(boolean ssl) {
        this.ssl = ssl;
        return this;
    }

    public String getNt() {
        return nt;
    }

    public JdbcConf setNt(String nt) {
        this.nt = nt;
        return this;
    }

    public Properties getProps() {
        return props;
    }

    public JdbcConf setProps(Properties props) {
        this.props = props;
        return this;
    }

    public String url() {
        StringBuilder builder = new StringBuilder();
        builder.append(jdbcType.fullProtocol())
                .append("://")
                .append(host);
        if (port > 0 && port < 65535) {
            builder.append(":")
                    .append(port);
        }
        builder.append("/");
        if (StringUtils.isNotBlank(database)) {
            builder.append(database);
        }
        if (urlParams != null && !urlParams.isEmpty()) {
            builder.append(jdbcType.databaseParamsSeparator);
            String joinedParams = urlParams.keySet().stream()
                    .sorted()
                    .map(s -> s + "=" + urlParams.get(s))
                    .collect(Collectors.joining(jdbcType.paramsDelimiter));
            builder.append(joinedParams);
        }

        return builder.toString();
    }

    public Object get(Object key) {
        return props.get(key);
    }

    public Object put(Object key, Object value) {
        return props.put(key, value);
    }

    public String getProperty(String key) {
        return props.getProperty(key);
    }

    public String getProperty(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    public String putUrlParam(String key, String value) {
        return urlParams.put(key, value);
    }

    public String putProperty(String key, String value) {
        return (String) props.setProperty(key, value);
    }

    public JdbcType getJdbcType() {
        return jdbcType;
    }

    public JdbcConf setJdbcType(JdbcType jdbcType) {
        this.jdbcType = jdbcType;
        return this;
    }

    public String getHost() {
        return host;
    }

    public JdbcConf setHost(String host) {
        this.host = host;
        return this;
    }

    public String getUser() {
        return user;
    }

    public JdbcConf setUser(String user) {
        this.user = user;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public JdbcConf setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getDatabase() {
        return database;
    }

    public JdbcConf setDatabase(String database) {
        this.database = database;
        return this;
    }

    public Map<String, String> getUrlParams() {
        return urlParams;
    }

    public JdbcConf setUrlParams(Map<String, String> urlParams) {
        this.urlParams = urlParams;
        return this;
    }

    public int getRowMax() {
        return this.rowMax;
    }

    public IJdbcConf setRowMax(int limit) {
        this.rowMax = limit;
        return this;
    }

}
