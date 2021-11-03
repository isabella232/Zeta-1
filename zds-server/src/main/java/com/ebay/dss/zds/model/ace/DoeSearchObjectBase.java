package com.ebay.dss.zds.model.ace;

public class DoeSearchObjectBase {

    private Integer status;
    private  String error;

    public Integer getStatus() {
        return status;
    }

    public DoeSearchObjectBase setStatus(Integer status) {
        this.status = status;
        return this;
    }

    public String getError() {
        return error;
    }

    public DoeSearchObjectBase setError(String error) {
        this.error = error;
        return this;
    }
}
