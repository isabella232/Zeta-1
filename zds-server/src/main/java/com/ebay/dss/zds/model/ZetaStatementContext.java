package com.ebay.dss.zds.model;

import com.ebay.dss.zds.common.JsonUtil;
import com.google.gson.JsonObject;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by tatian on 2021-01-14.
 */
public class ZetaStatementContext {

  public long id;
  public long requestId;
  public String statement;
  public int seq;
  public String status;
  public String preference;
  public String jobUrl;
  public String proxyUser;
  public String platform;

  public long createDt;
  public long startDt;
  public long updateDt;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public long getRequestId() {
    return requestId;
  }

  public void setRequestId(long requestId) {
    this.requestId = requestId;
  }

  public String getStatement() {
    return statement;
  }

  public void setStatement(String statement) {
    this.statement = statement;
  }

  public int getSeq() {
    return seq;
  }

  public void setSeq(int seq) {
    this.seq = seq;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getPreference() {
    return preference;
  }

  public void setPreference(String preference) {
    this.preference = preference;
  }

  public String getJobUrl() {
    return jobUrl;
  }

  public void setJobUrl(String jobUrl) {
    this.jobUrl = jobUrl;
  }

  public String getProxyUser() {
    return proxyUser;
  }

  public void setProxyUser(String proxyUser) {
    this.proxyUser = proxyUser;
  }

  public String getPlatform() {
    return platform;
  }

  public void setPlatform(String platform) {
    this.platform = platform;
  }

  public long getCreateDt() {
    return createDt;
  }

  public void setCreateDt(long createDt) {
    this.createDt = createDt;
  }

  public long getStartDt() {
    return startDt;
  }

  public void setStartDt(long startDt) {
    this.startDt = startDt;
  }

  public long getUpdateDt() {
    return updateDt;
  }

  public void setUpdateDt(long updateDt) {
    this.updateDt = updateDt;
  }

  public String toJson() {
    return JsonUtil.GSON.toJson(this);
  }
}
