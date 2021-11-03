package com.ebay.dss.zds.model.schedule;


import java.util.Date;


public class ScheduleHistoryDto {

  private Long id;

  private Long jobId;

  private Long jobHistoryId;

  private String log;

  private Date startTime;

  private Date endTime;

  private Date runTime;

  private JobRunStatusInfo jobRunStatusInfo;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getJobId() {
    return jobId;
  }

  public void setJobId(Long jobId) {
    this.jobId = jobId;
  }

  public Long getJobHistoryId() {
    return jobHistoryId;
  }

  public void setJobHistoryId(Long jobHistoryId) {
    this.jobHistoryId = jobHistoryId;
  }

  public String getLog() {
    return log;
  }

  public void setLog(String log) {
    this.log = log;
  }

  public Date getStartTime() {
    return startTime;
  }

  public void setStartTime(Date startTime) {
    this.startTime = startTime;
  }

  public Date getEndTime() {
    return endTime;
  }

  public void setEndTime(Date endTime) {
    this.endTime = endTime;
  }

  public Date getRunTime() {
    return runTime;
  }

  public void setRunTime(Date runTime) {
    this.runTime = runTime;
  }

  public JobRunStatusInfo getJobRunStatusInfo() {
    return jobRunStatusInfo;
  }

  public void setJobRunStatusInfo(JobRunStatusInfo jobRunStatusInfo) {
    this.jobRunStatusInfo = jobRunStatusInfo;
  }
}
