package com.ebay.dss.zds.exception;

public class EntityIsNullException extends ApplicationBaseException{
    public EntityIsNullException(String message) {
        super(ErrorCode.ENTITY_IS_NULL, message);
    }
}
