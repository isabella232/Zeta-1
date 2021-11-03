package com.ebay.dss.zds.service.schedule.track;


import com.ebay.dss.zds.model.ZetaOperationLog.OperationType;

import java.lang.annotation.*;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLogDetail {

  String description() default "";

  String operationInterface();

  OperationType operationType() default OperationType.SCHEDULER;
}
