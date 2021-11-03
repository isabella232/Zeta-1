package com.ebay.dss.zds.exception;

/**
 * Created by tatian on 2018/5/23.
 */
public class ConfigurationNotFoundException extends ApplicationBaseException{
    public ConfigurationNotFoundException(String message) {
        super(ErrorCode.CONFIGURATION_EXCEPTION, message);
    }

    public ConfigurationNotFoundException(Exception ex) {
        super(ErrorCode.CONFIGURATION_EXCEPTION, ex.getMessage());
    }
}
