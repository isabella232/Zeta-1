package com.ebay.dss.zds.websocket.notebook.dto;

/**
 * Created by tatian on 2019-01-23.
 */
public class ExecuteDumpReq extends ExecuteCodeReq{

    public int limit;

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

}
