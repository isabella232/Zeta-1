package com.ebay.dss.zds.exception;
/**
* Created by zhouhuang on 2018年11月1日 
*/
public class ScheduleException extends ApplicationBaseException{

	public ScheduleException(String message) {
		super(ErrorCode.SCHEDULE_EXCEPTION, message);
	}

}
