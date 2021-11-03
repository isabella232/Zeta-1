package com.ebay.dss.zds.model.schedule;


import java.util.Date;


public class ScheduleJobDto {

  private Long id;

  private String jobName;

  private String nt;

  private String scheduleTime;

  private String cronExpression;

  private String type;

  private String task;

  private String dependency;

  private String dependencySignal;

  // 0: inactive 1: active
  private int status;

  private String ccAddr;

  private Date lastRunTime;

  private Date nextRunTime;

  private Date createTime;

  private Date updateTime;

  private int mailSwitch;

  private String authInfo;

  private int failTimesToBlock;

  private boolean autoRetry;

  private JobRunStatusInfo jobRunStatusInfo;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getJobName() {
    return jobName;
  }

  public void setJobName(String jobName) {
    this.jobName = jobName;
  }

  public String getNt() {
    return nt;
  }

  public void setNt(String nt) {
    this.nt = nt;
  }

  public String getScheduleTime() {
    return scheduleTime;
  }

  public void setScheduleTime(String scheduleTime) {
    this.scheduleTime = scheduleTime;
  }

  public String getCronExpression() {
    return cronExpression;
  }

  public void setCronExpression(String cronExpression) {
    this.cronExpression = cronExpression;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getTask() {
    return task;
  }

  public void setTask(String task) {
    this.task = task;
  }

  public String getDependency() {
    return dependency;
  }

  public void setDependency(String dependency) {
    this.dependency = dependency;
  }

  public int getStatus() {
    return status;
  }

  public void setStatus(int status) {
    this.status = status;
  }

  public String getCcAddr() {
    return ccAddr;
  }

  public void setCcAddr(String ccAddr) {
    this.ccAddr = ccAddr;
  }

  public Date getLastRunTime() {
    return lastRunTime;
  }

  public void setLastRunTime(Date lastRunTime) {
    this.lastRunTime = lastRunTime;
  }

  public Date getNextRunTime() {
    return nextRunTime;
  }

  public void setNextRunTime(Date nextRunTime) {
    this.nextRunTime = nextRunTime;
  }

  public Date getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }

  public Date getUpdateTime() {
    return updateTime;
  }

  public void setUpdateTime(Date updateTime) {
    this.updateTime = updateTime;
  }

  public int getMailSwitch() {
    return mailSwitch;
  }

  public void setMailSwitch(int mailSwitch) {
    this.mailSwitch = mailSwitch;
  }

  public String getAuthInfo() {
    return authInfo;
  }

  public void setAuthInfo(String authInfo) {
    this.authInfo = authInfo;
  }

  public JobRunStatusInfo getJobRunStatusInfo() {
    return jobRunStatusInfo;
  }

  public void setJobRunStatusInfo(JobRunStatusInfo jobRunStatusInfo) {
    this.jobRunStatusInfo = jobRunStatusInfo;
  }

  public String getDependencySignal() {
    return dependencySignal;
  }

  public void setDependencySignal(String dependencySignal) {
    this.dependencySignal = dependencySignal;
  }

  public int getFailTimesToBlock() {
    return failTimesToBlock;
  }

  public void setFailTimesToBlock(int failTimesToBlock) {
    this.failTimesToBlock = failTimesToBlock;
  }

  public boolean isAutoRetry() {
    return autoRetry;
  }

  public void setAutoRetry(boolean autoRetry) {
    this.autoRetry = autoRetry;
  }
}
