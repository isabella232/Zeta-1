package com.ebay.dss.zds.model.schedule;

import java.util.Date;

import javax.persistence.*;
import javax.persistence.Column;
import javax.persistence.Table;

/**
 * Created by zhouhuang on 2018年11月2日
 */
@Entity
@Table(name = "schedule_history")
public class ScheduleHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long jobHistoryId;

  //0: in progress 1: pass 2: fail
//    private int status;

  private String log;

  private Date startTime;

  private Date endTime;

  private Date runTime;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(
      name = "jobId",
      referencedColumnName = "id")
  private ScheduleJob scheduleJob;

  @Column
  @Enumerated(EnumType.STRING)
  private JobRunStatus jobRunStatus;

  @Column
  @Enumerated(EnumType.STRING)
  private JobOperation jobOperation;

  public enum JobRunStatus {
    NOTSTART,
    PENDING,
    WAITING,
    RUNNING,
    CANCELED,
    SUCCESS,
    FAIL,
    AUTORETRY,
    AUTORETRYWAITING,
    RETRYSUCCESS,
    RETRYFAIL
  }


  public enum JobOperation {
    SKIP, CANCEL, RUN
  }

  /**
   * @return the id
   */
  public Long getId() {
    return id;
  }

  /**
   * @param id the id to set
   */
  public void setId(Long id) {
    this.id = id;
  }

  public ScheduleJob getScheduleJob() {
    return scheduleJob;
  }

  public void setScheduleJob(ScheduleJob scheduleJob) {
    this.scheduleJob = scheduleJob;
  }


  /**
   * @return the jobHistoryId
   */
  public Long getJobHistoryId() {
    return jobHistoryId;
  }

  /**
   * @param jobHistoryId the jobHistoryId to set
   */
  public void setJobHistoryId(Long jobHistoryId) {
    this.jobHistoryId = jobHistoryId;
  }

  /**
   * @return the startTime
   */
  public Date getStartTime() {
    return startTime;
  }

  /**
   * @param startTime the startTime to set
   */
  public void setStartTime(Date startTime) {
    this.startTime = startTime;
  }

  /**
   * @return the endTime
   */
  public Date getEndTime() {
    return endTime;
  }

  /**
   * @param endTime the endTime to set
   */
  public void setEndTime(Date endTime) {
    this.endTime = endTime;
  }

  /**
   * @return the log
   */
  public String getLog() {
    return log;
  }

  /**
   * @param log the log to set
   */
  public void setLog(String log) {
    this.log = log;
  }

  @Override
  public String toString() {
    return String.format("ScheduleHistory[id = %s, jobId = %s, jobHistoryId = %s, status = %s, startTime = %s]"
        , id, scheduleJob.getId(), jobHistoryId, jobRunStatus, startTime);
  }

  public JobRunStatus getJobRunStatus() {
    return jobRunStatus;
  }

  public void setJobRunStatus(JobRunStatus jobRunStatus) {
    this.jobRunStatus = jobRunStatus;
  }

  public Date getRunTime() {
    return runTime;
  }

  public void setRunTime(Date runTime) {
    this.runTime = runTime;
  }

  public JobOperation getJobOperation() {
    return jobOperation;
  }

  public void setJobOperation(JobOperation jobOperation) {
    this.jobOperation = jobOperation;
  }
}
