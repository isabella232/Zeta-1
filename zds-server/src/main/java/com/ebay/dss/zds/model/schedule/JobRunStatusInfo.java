package com.ebay.dss.zds.model.schedule;

import com.ebay.dss.zds.model.schedule.ScheduleHistory.JobRunStatus;

import java.util.Map;


public class JobRunStatusInfo {

  private JobRunStatus jobRunStatus;
  private Map<String,Object> info;

  public JobRunStatus getJobRunStatus() {
    return jobRunStatus;
  }

  public void setJobRunStatus(JobRunStatus jobRunStatus) {
    this.jobRunStatus = jobRunStatus;
  }

  public Map<String, Object> getInfo() {
    return info;
  }

  public void setInfo(Map<String, Object> info) {
    this.info = info;
  }
}
