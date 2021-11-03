package com.ebay.dss.zds.service.schedule.hook;

import com.ebay.dss.zds.model.schedule.ScheduleHistory;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ScheduleHookSubject {

  private final static Logger LOGGER = LoggerFactory.getLogger(ScheduleHookSubject.class);
  public static ScheduleHookSubject ScheduleHook;

  static {
    ScheduleHook = new ScheduleHookSubject();
  }

  private List<ScheduleHistoryObserver> observers = Lists.newArrayList();

  public void tearDown(ScheduleHistory history) {
    notifyAllObservers(history);
  }

  public void addObserver(ScheduleHistoryObserver observer, int... index) {
    if (ArrayUtils.isNotEmpty(index)) {
      observers.add(index[0], observer);
    } else {
      observers.add(observer);
    }
  }

  public List<ScheduleHistoryObserver> getObservers() {
    return observers;
  }

  public void notifyAllObservers(ScheduleHistory scheduleHistory) {
    for (ScheduleHistoryObserver observer : observers) {
      LOGGER.info("Notify observers {}", observer.getClass().getName());
      observer.update(scheduleHistory);
    }
  }

}
