package com.ebay.dss.zds.exception;


/**
 * Created by wenliu2 on 4/3/18.
 */
public class NotebookException extends ApplicationBaseException {
	public NotebookException(String message) {
		super(ErrorCode.NOTEBOOK_EXCEPTION, message);
	}
}
