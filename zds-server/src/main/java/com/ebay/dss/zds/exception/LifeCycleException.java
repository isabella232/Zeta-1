package com.ebay.dss.zds.exception;

/**
 * Created by tatian on 2018/5/3.
 */
public class LifeCycleException extends ApplicationBaseException{
    public LifeCycleException(String message) {
        super(ErrorCode.LIFECYCLE_EXCEPTION, message);
    }

    public LifeCycleException(Exception ex) {
        super(ErrorCode.LIFECYCLE_EXCEPTION, ex.getMessage());
    }
}
