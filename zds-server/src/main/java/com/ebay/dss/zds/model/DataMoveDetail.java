package com.ebay.dss.zds.model;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonBackReference;

/**
 * Created by zhouhuang on Apr 27, 2018
 */
@Entity
@Table(name = "data_move_detail")
public class DataMoveDetail {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "is_drop")
  private int isDrop;

  @Column(name = "is_convert")
  private int isConvert;

  private String filter;

  private String query;

  private String ddl;

  @Column(name = "td_bridge_avro")
  private String tdBridgeAvro;

  @Column(name = "spark_sql")
  private String sparkSql;

  @Column(name = "task_id")
  private int taskId;

  @Column(name = "hdfs_path")
  private String hdfsPath;

  @Column(name = "cre_date")
  private Date createDate;

  @Column(name = "touchfile_id")
  private int touchfileId;

  private String errorLog;

  private String queue;

  private int step;

  @Column(name = "view_db")
  private String viewDb;

  @OneToOne(targetEntity = History.class, cascade = {CascadeType.ALL}, fetch = FetchType.EAGER)
  @JoinColumn(name = "history_id", referencedColumnName = "history_id")
  private History history;

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
   * @return the filter
   */
  public String getFilter() {
    return filter;
  }

  /**
   * @param filter the filter to set
   */
  public void setFilter(String filter) {
    this.filter = filter;
  }

  /**
   * @return the createDate
   */
  public Date getCreateDate() {
    return createDate;
  }

  /**
   * @param date the createDate to set
   */
  public void setCreateDate(Date date) {
    this.createDate = date;
  }

  /**
   * @return the history
   */
  @JsonBackReference
  public History getHistory() {
    return history;
  }

  /**
   * @param history the history to set
   */
  @JsonBackReference
  public void setHistory(History history) {
    this.history = history;
  }

  /**
   * @return the hdfsPath
   */
  public String getHdfsPath() {
    return hdfsPath;
  }

  /**
   * @param hdfsPath the hdfsPath to set
   */
  public void setHdfsPath(String hdfsPath) {
    this.hdfsPath = hdfsPath;
  }

  /**
   * @return the query
   */
  public String getQuery() {
    return query;
  }

  /**
   * @param query the query to set
   */
  public void setQuery(String query) {
    this.query = query;
  }

  /**
   * @return the isDrop
   */
  public int getIsDrop() {
    return isDrop;
  }

  /**
   * @param isDrop the isDrop to set
   */
  public void setIsDrop(int isDrop) {
    this.isDrop = isDrop;
  }

  /**
   * @return the taskId
   */
  public int getTaskId() {
    return taskId;
  }

  /**
   * @param taskId the taskId to set
   */
  public void setTaskId(int taskId) {
    this.taskId = taskId;
  }

  /**
   * @return the ddl
   */
  public String getDdl() {
    return ddl;
  }

  /**
   * @param ddl the ddl to set
   */
  public void setDdl(String ddl) {
    this.ddl = ddl;
  }

  /**
   * @return the tdBridgeAvro
   */
  public String getTdBridgeAvro() {
    return tdBridgeAvro;
  }

  /**
   * @param tdBridgeAvro the tdBridgeAvro to set
   */
  public void setTdBridgeAvro(String tdBridgeAvro) {
    this.tdBridgeAvro = tdBridgeAvro;
  }

  /**
   * @return the sparkSql
   */
  public String getSparkSql() {
    return sparkSql;
  }

  /**
   * @param sparkSql the sparkSql to set
   */
  public void setSparkSql(String sparkSql) {
    this.sparkSql = sparkSql;
  }

  /**
   * @return the touchfileId
   */
  public int getTouchfileId() {
    return touchfileId;
  }

  /**
   * @param touchfileId the touchfileId to set
   */
  public void setTouchfileId(int touchfileId) {
    this.touchfileId = touchfileId;
  }


  public String getErrorLog() {
    return errorLog;
  }

  public void setErrorLog(String errorLog) {
    this.errorLog = errorLog;
  }

  public String getQueue() {
    return queue;
  }

  public void setQueue(String queue) {
    this.queue = queue;
  }

  public int getStep() {
    return step;
  }

  public void setStep(int step) {
    this.step = step;
  }

  public int getIsConvert() {
    return isConvert;
  }

  public void setIsConvert(int isConvert) {
    this.isConvert = isConvert;
  }

  public boolean isConvert() {
    return isConvert == 1 ? true : false;
  }

  public String getViewDb() {
    return viewDb;
  }

  public void setViewDb(String viewDb) {
    this.viewDb = viewDb;
  }

  @Override
  public String toString() {
    return String.format("DataMoveDetail[id = %s, hid = %s, step = %s, taskId = %s, nT = %s, sourceT = %s, sourceP = %s, targetT = %s, targetP = %s]", id, history.getHistoryId(), step, taskId, history.getNt(), history.getSourceTable(), history.getSourcePlatform(), history.getTargetTable(), history.getTargetPlatform());
  }
}