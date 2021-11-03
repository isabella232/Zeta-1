package com.ebay.dss.zds.service.schedule.track;


import com.ebay.dss.zds.common.JsonUtil;
import com.ebay.dss.zds.dao.ZetaOperationLogRepository;
import com.ebay.dss.zds.model.ZetaOperationLog;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Aspect
@Component
public class OperatorLogAspect {
  private static final Logger LOGGER = LoggerFactory.getLogger(OperatorLogAspect.class);

  @Autowired
  private ZetaOperationLogRepository zetaOperationLogRepository;

  @Pointcut("@annotation(com.ebay.dss.zds.service.schedule.track.OperationLogDetail)")
  public void operatorLog() {
  }

  @AfterReturning(value = "operatorLog()", returning = "zetaOperationLog")
  public void afterReturning(JoinPoint joinPoint, ZetaOperationLog zetaOperationLog) {

    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    OperationLogDetail annotation = signature.getMethod()
        .getAnnotation(OperationLogDetail.class);
    try {
      LOGGER.info("Operation Interface :{}, Operation Args: {}"
          , annotation.operationInterface(), joinPoint.getArgs());

      if (!annotation.description().isEmpty()) {
        zetaOperationLog.setDescription(annotation.description());
      }
      zetaOperationLog.setOperationType(annotation.operationType());
      zetaOperationLog.setOperationInterface(annotation.operationInterface());
      zetaOperationLog.setCreateTime(new Date());
      LOGGER.info("Zeta Operation Log : {}", JsonUtil.toJson(zetaOperationLog));
      zetaOperationLogRepository.save(zetaOperationLog);
    } catch (Exception e) {
      LOGGER.error("Track Operation Log failed!", e);

    }
  }

}
