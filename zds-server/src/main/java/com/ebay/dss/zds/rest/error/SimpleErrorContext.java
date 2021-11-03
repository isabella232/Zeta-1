package com.ebay.dss.zds.rest.error;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public class SimpleErrorContext implements ErrorContext {

    @JsonUnwrapped
    private Object object;

    public Object getObject() {
        return object;
    }

    public SimpleErrorContext setObject(Object object) {
        this.object = object;
        return this;
    }
}
