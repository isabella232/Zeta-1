package com.ebay.dss.zds.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;

/**
 * Created by zhouhuang on Apr 26, 2018
 */
@Entity
@Table(name = "history")
public class History {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "history_id")
  private Long historyId;

  @Column(name = "nt")
  @NotBlank(message = "NT Can't Be Empty")
  @JsonProperty("nT")
  private String nt;

  @Column(name = "source_table")
  @NotBlank(message = "sourceTable Can't Be Empty")
  private String sourceTable;

  @Column(name = "target_table")
  @NotBlank(message = "targetTable Can't Be Empty")
  private String targetTable;

  @Column(name = "source_platform")
  @NotBlank(message = "sourcePlatform Can't Be Empty")
  private String sourcePlatform;

  @Column(name = "target_platform")
  @NotBlank(message = "targetPlatform Can't Be Empty")
  private String targetPlatform;

  // 0: in progress 1: pass 2: fail
  private int status;

  // 1: data move 2: data valid 3：local 4: vdm
  private int type;

  private String log;

  private String crontab;

  @Column(name = "last_run_date")
  private Date lastRunDate;

  @Column(name = "cre_date")
  private Date createDate;

  @Column(name = "touch_file")
  private String touchFile;


  @Column(name = "start_time")
  private Date startTime;

  @Column(name = "end_time")
  private Date endTime;

  @OneToOne(mappedBy = "history", cascade = {CascadeType.ALL})
  private DataMoveDetail dataMoveDetail;

  @OneToOne(mappedBy = "history", cascade = {CascadeType.ALL})
  private DataValidateDetail dataValidateDetail;

  /**
   * @return the historyId
   */
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long getHistoryId() {
    return historyId;
  }

  /**
   * @param historyId the historyId to set
   */
  public void setHistoryId(Long historyId) {
    this.historyId = historyId;
  }

  /**
   * @return the nT
   */
  public String getNt() {
    return nt;
  }

  /**
   * @param nT the nT to set
   */
  public void setNt(String nT) {
    this.nt = nT;
  }

  /**
   * @return the sourceTable
   */
  public String getSourceTable() {
    return sourceTable;
  }

  /**
   * @param sourceTable the sourceTable to set
   */
  public void setSourceTable(String sourceTable) {
    this.sourceTable = sourceTable;
  }

  /**
   * @return the targetTable
   */
  public String getTargetTable() {
    return targetTable;
  }

  /**
   * @param targetTable the targetTable to set
   */
  public void setTargetTable(String targetTable) {
    this.targetTable = targetTable;
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
  public int getType() {
    return type;
  }

  /**
   * @param type the type to set
   */
  public void setType(int type) {
    this.type = type;
  }

  /**
   * @return the createDate
   */
  public Date getCreateDate() {
    return createDate;
  }

  /**
   * @param createDate the createDate to set
   */
  public void setCreateDate(Date createDate) {
    this.createDate = createDate;
  }

  /**
   * @return the dataMoveDetail
   */
  public DataMoveDetail getDataMoveDetail() {
    return dataMoveDetail;
  }

  /**
   * @param dataMoveDetail the dataMoveDetail to set
   */
  public void setDataMoveDetail(DataMoveDetail dataMoveDetail) {
    this.dataMoveDetail = dataMoveDetail;
  }

  /**
   * @return the dataValidateDetail
   */
  public DataValidateDetail getDataValidateDetail() {
    return dataValidateDetail;
  }

  /**
   * @param dataValidateDetail the dataValidateDetail to set
   */
  public void setDataValidateDetail(DataValidateDetail dataValidateDetail) {
    this.dataValidateDetail = dataValidateDetail;
  }

  /**
   * @return the sourcePlatform
   */
  public String getSourcePlatform() {
    return sourcePlatform;
  }

  /**
   * @param sourcePlatform the sourcePlatform to set
   */
  public void setSourcePlatform(String sourcePlatform) {
    this.sourcePlatform = sourcePlatform;
  }

  /**
   * @return the targetPlatform
   */
  public String getTargetPlatform() {
    return targetPlatform;
  }

  /**
   * @param targetPlatform the targetPlatform to set
   */
  public void setTargetPlatform(String targetPlatform) {
    this.targetPlatform = targetPlatform;
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

  /**
   * @return the crontab
   */
  public String getCrontab() {
    return crontab;
  }

  /**
   * @param crontab the crontab to set
   */
  public void setCrontab(String crontab) {
    this.crontab = crontab;
  }

  /**
   * @return the lastRunDate
   */
  public Date getLastRunDate() {
    return lastRunDate;
  }

  /**
   * @param lastRunDate the lastRunDate to set
   */
  public void setLastRunDate(Date lastRunDate) {
    this.lastRunDate = lastRunDate;
  }

  /**
   * @return the touchFile
   */
  public String getTouchFile() {
    return touchFile;
  }

  /**
   * @param touchFile the touchFile to set
   */
  public void setTouchFile(String touchFile) {
    this.touchFile = touchFile;
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

  public String getVDMSourcePlatform() {
    return "numozart".equalsIgnoreCase(sourcePlatform) ? "mozart" : sourcePlatform;
  }

}
