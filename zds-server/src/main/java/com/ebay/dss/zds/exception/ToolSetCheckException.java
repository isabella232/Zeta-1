package com.ebay.dss.zds.exception;

/**
 * Created by wenliu2 on 4/3/18.
 */
public class ToolSetCheckException extends ApplicationBaseException {
	public ToolSetCheckException(String message) {
		super(ErrorCode.TOOLSET_EXCEPTION, message);
	}
}
