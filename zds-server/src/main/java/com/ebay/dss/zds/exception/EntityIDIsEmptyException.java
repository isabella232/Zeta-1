package com.ebay.dss.zds.exception;

public class EntityIDIsEmptyException extends ApplicationBaseException {

    public EntityIDIsEmptyException(String message) {
        super(ErrorCode.ENTITY_ID_EMPTY, message);
    }
}
