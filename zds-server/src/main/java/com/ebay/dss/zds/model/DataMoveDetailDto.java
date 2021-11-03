package com.ebay.dss.zds.model;

import java.util.Date;
import java.util.List;

public class DataMoveDetailDto {

  private Long id;

  private int isDrop;

  private int isConvert;

  private String filter;

  private String query;

  private String ddl;

  private String tdBridgeAvro;

  private String sparkSql;

  private int taskId;

  private String hdfsPath;

  private Date createDate;

  private int touchfileId;

  private String errorLog;

  private String queue;

  private int step;

  private String viewDb;

  private History history;

  List<String> overrideTables;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public int getIsDrop() {
    return isDrop;
  }

  public void setIsDrop(int isDrop) {
    this.isDrop = isDrop;
  }

  public int getIsConvert() {
    return isConvert;
  }

  public void setIsConvert(int isConvert) {
    this.isConvert = isConvert;
  }

  public String getFilter() {
    return filter;
  }

  public void setFilter(String filter) {
    this.filter = filter;
  }

  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public String getDdl() {
    return ddl;
  }

  public void setDdl(String ddl) {
    this.ddl = ddl;
  }

  public String getTdBridgeAvro() {
    return tdBridgeAvro;
  }

  public void setTdBridgeAvro(String tdBridgeAvro) {
    this.tdBridgeAvro = tdBridgeAvro;
  }

  public String getSparkSql() {
    return sparkSql;
  }

  public void setSparkSql(String sparkSql) {
    this.sparkSql = sparkSql;
  }

  public int getTaskId() {
    return taskId;
  }

  public void setTaskId(int taskId) {
    this.taskId = taskId;
  }

  public String getHdfsPath() {
    return hdfsPath;
  }

  public void setHdfsPath(String hdfsPath) {
    this.hdfsPath = hdfsPath;
  }

  public Date getCreateDate() {
    return createDate;
  }

  public void setCreateDate(Date createDate) {
    this.createDate = createDate;
  }

  public int getTouchfileId() {
    return touchfileId;
  }

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

  public String getViewDb() {
    return viewDb;
  }

  public void setViewDb(String viewDb) {
    this.viewDb = viewDb;
  }

  public History getHistory() {
    return history;
  }

  public void setHistory(History history) {
    this.history = history;
  }

  public List<String> getOverrideTables() {
    return overrideTables;
  }

  public void setOverrideTables(List<String> overrideTables) {
    this.overrideTables = overrideTables;
  }
}
