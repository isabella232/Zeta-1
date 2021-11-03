package com.ebay.dss.zds.service.schedule.hook;

import com.ebay.dss.zds.model.schedule.ScheduleHistory;
import com.ebay.dss.zds.service.schedule.Scheduler;

public abstract class ScheduleHistoryObserver {
  protected ScheduleHookSubject subject;

  public abstract void update(ScheduleHistory history);
}
