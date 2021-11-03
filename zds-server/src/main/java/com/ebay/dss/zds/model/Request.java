package com.ebay.dss.zds.model;

import java.util.List;
import java.util.Properties;

public class Request {
    private String sql;
    private List<String> sqlLists;
    private String tableName;
    private String sqlFileName;
//    private Properties properties;
    private List<String> tableLists;

    public List<String> getTableLists() {
        return tableLists;
    }

    public void setTableLists(List<String> tableLists) {
        this.tableLists = tableLists;
    }

//    public Properties  getProperties() {
//        return properties;
//    }
//
//    public void setProperties(Properties  properties) {
//        this.properties = properties;
//    }
    public String getSqlFileName() {
        return sqlFileName;
    }

    public void setSqlFileName(String sqlFileName) {
        this.sqlFileName = sqlFileName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }
    public List<String> getSqlLists() {
        return sqlLists;
    }

    public void setSqlLists(List<String> sqlLists) {
        this.sqlLists = sqlLists;
    }
}
