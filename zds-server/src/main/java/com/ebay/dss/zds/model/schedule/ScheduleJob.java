package com.ebay.dss.zds.model.schedule;

import com.ebay.dss.zds.common.JsonUtil;
import com.ebay.dss.zds.exception.ToolSetCheckException;
import com.ebay.dss.zds.model.authorization.ZetaAuthorization;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.persistence.*;
import javax.persistence.Column;
import javax.persistence.Table;

/**
 * Created by zhouhuang on 2018年11月1日
 */
@Entity
@Table(name = "schedule_job")
public class ScheduleJob extends ZetaAuthorization {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String jobName;

  @Column(nullable = false)
  private String nt;

  @Column(nullable = false)
  private String scheduleTime;

  private String cronExpression;

  // Notebook/DataMove/DataValidate
  @Column(nullable = false)
  private String type;

  @Column(nullable = false)
  private String task;

  private String dependency;

  private String dependencySignal;

  // 0: inactive 1: active
  @Column(nullable = false)
  private int status;

  private String ccAddr;

  private Date lastRunTime;

  private Date nextRunTime;

  private Date createTime;

  private Date updateTime;

  private int mailSwitch;

  private String authInfo;

  private int failTimesToBlock;

  @Column(columnDefinition="tinyint(1) default 0")
  private boolean autoRetry;

  @OneToMany(
      mappedBy = "scheduleJob",
      fetch = FetchType.LAZY,
      cascade = CascadeType.ALL,
      orphanRemoval = true)
  @JsonIgnore
  private Set<ScheduleHistory> scheduleHistories;

  public Set<ScheduleHistory> getScheduleHistories() {
    return scheduleHistories;
  }

  public void setScheduleHistories(Set<ScheduleHistory> scheduleHistories) {
    this.scheduleHistories = scheduleHistories;
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

  /**
   * @return the jobName
   */
  public String getJobName() {
    return jobName;
  }

  /**
   * @param jobName the jobName to set
   */
  public void setJobName(String jobName) {
    this.jobName = jobName;
  }

  public String getScheduleTime() {
    return scheduleTime;
  }

  public void setScheduleTime(String scheduleTime) {
    this.scheduleTime = scheduleTime;
  }

  /**
   * @return the cronExpression
   */
  public String getCronExpression() {
    return cronExpression;
  }

  /**
   * @param cronExpression the cronExpression to set
   */
  public void setCronExpression(String cronExpression) {
    this.cronExpression = cronExpression;
  }

  /**
   * @return the task
   */
  public String getTask() {
    return task;
  }

  /**
   * @param task the task to set
   */
  public void setTask(String task) {
    this.task = task;
  }

  /**
   * @return the lastRunTime
   */
  public Date getLastRunTime() {
    return lastRunTime;
  }

  /**
   * @param lastRunTime the lastRunTime to set
   */
  public void setLastRunTime(Date lastRunTime) {
    this.lastRunTime = lastRunTime;
  }

  /**
   * @return the nextRunTime
   */
  public Date getNextRunTime() {
    return nextRunTime;
  }

  /**
   * @param nextRunTime the nextRunTime to set
   */
  public void setNextRunTime(Date nextRunTime) {
    this.nextRunTime = nextRunTime;
  }

  /**
   * @return the createTime
   */
  public Date getCreateTime() {
    return createTime;
  }

  /**
   * @param createTime the createTime to set
   */
  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }

  /**
   * @return the updateTime
   */
  public Date getUpdateTime() {
    return updateTime;
  }

  /**
   * @param updateTime the updateTime to set
   */
  public void setUpdateTime(Date updateTime) {
    this.updateTime = updateTime;
  }

  /**
   * @return the status
   */
  public int getStatus() {
    return status;
  }

  /**
   * @param status the status to set
   */
  public void setStatus(int status) {
    this.status = status;
  }

  /**
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * @param type the type to set
   */
  public void setType(String type) {
    this.type = type;
  }

  /**
   * @return the nt
   */
  public String getNt() {
    return nt;
  }

  /**
   * @param nT the nt to set
   */
  public void setNt(String nT) {
    this.nt = nT;
  }

  public String getCcAddr() {
    return ccAddr;
  }

  public void setCcAddr(String ccAddr) {
    this.ccAddr = ccAddr;
  }

  public List<String> parseCcAddr() {
    try {
      return StringUtils.isNotBlank(ccAddr)
          ? JsonUtil.fromJson(ccAddr, List.class)
          : Lists.newArrayList();
    } catch (ToolSetCheckException e) {
      return Lists.newArrayList();
    }
  }

  @Override
  public String toString() {
    return String.format("ScheduleJob[id = %s, jobName = %s, nT = %s, type = %s]"
        , id, jobName, nt, type);
  }

  public boolean equals(ScheduleJob sJob) {
    return sJob.getJobName().equals(jobName)
        && sJob.getTask().equals(task)
        && sJob.getCronExpression().equals(cronExpression)
        && sJob.getType().equals(type)
        && sJob.getNt().equals(nt)
        && sJob.getScheduleTime().equals(scheduleTime);
  }


  public String getDependency() {
    return dependency;
  }

  public void setDependency(String dependency) {
    this.dependency = dependency;
  }

  public int getMailSwitch() {
    return mailSwitch;
  }

  public void setMailSwitch(int mailSwitch) {
    this.mailSwitch = mailSwitch;
  }

  @Override
  public String getAuthInfo() {
    return authInfo;
  }

  @Override
  public void setAuthInfo(String authInfo) {
    this.authInfo = authInfo;
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
