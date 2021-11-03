package com.ebay.dss.zds.service.schedule;


import com.ebay.dss.zds.model.ZetaOperationLog;
import com.ebay.dss.zds.service.schedule.track.OperationLogDetail;
import org.springframework.stereotype.Service;

@Service
public class ScheduleOperationTrack {


  @OperationLogDetail(operationInterface = "ScheduleOperationTrack.trackOperationLog")
  public ZetaOperationLog trackOperationLog(String runId, String nt, String operation, String... comments) {
    String comment = comments.length > 0 ? comments[0] : null;
    return new ZetaOperationLog(runId, nt, operation, comment);
  }
}
