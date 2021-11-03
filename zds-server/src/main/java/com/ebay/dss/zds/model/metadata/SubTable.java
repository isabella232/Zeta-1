package com.ebay.dss.zds.model.metadata;

/**
 * Created by tianrsun on 2018/11/13.
 */
public class SubTable {
    private String dbName;
    private String tableName;
    private boolean disable;
    public SubTable(){
        this.disable = false;
    }
    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public boolean isDisable() {
        return disable;
    }

    public void setDisable(boolean disable) {
        this.disable = disable;
    }
}
