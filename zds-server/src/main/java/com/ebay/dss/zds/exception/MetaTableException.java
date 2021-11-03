package com.ebay.dss.zds.exception;

public class MetaTableException extends ApplicationBaseException{

	public MetaTableException(String message) {
		super(ErrorCode.METATABLE_EXCEPTION, message);
	}

}
