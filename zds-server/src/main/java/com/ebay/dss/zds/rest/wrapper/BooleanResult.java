package com.ebay.dss.zds.rest.wrapper;

public class BooleanResult {

    private Boolean result;

    public BooleanResult(Boolean result) {
        this.result = result;
    }

    public BooleanResult() {
    }

    public Boolean getResult() {
        return result;
    }

    public BooleanResult setResult(Boolean result) {
        this.result = result;
        return this;
    }
}
