package com.ebay.dss.zds.model.metatable;


import com.ebay.dss.zds.model.authorization.ZetaAuthorization;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import java.util.*;

@Entity
@Table(name = "zeta_meta_table")
public class ZetaMetaTable extends ZetaAuthorization {

  @Id
  @Column
  private String id;

  @Column
  @NotEmpty
  private String metaTableName;

  @Column
  private String nt;

  @Column
  private String platform;

  @Column
  private String account;

  @Column
  private String db;

  @Column
  private String tbl;

  @Column
  @NotEmpty
  private String schemaInfo;

  @Column
  private String hadoopSchemaInfo;

  @Column
  private String authInfo;

  @Column
  @Enumerated(EnumType.STRING)
  private MetaTableType metaTableType;

  @Column
  private String cron;

  @Column
  @Enumerated(EnumType.STRING)
  private MetaTableStatus metaTableStatus;

  @Column
  private Date createTime;

  @Column
  private Date updateTime;

  @Column
  private Date syncTime;

  @Column
  private String failLog;

  @Column
  private String lastModifier;

  private String path;

  public enum MetaTableType {
    HDM, PROD
  }

  public enum MetaTableStatus {
    CREATED, SYNCING, SUCCESS, REGISTER_FAIL, LOAD_FAIL, DELETED
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getMetaTableName() {
    return metaTableName;
  }

  public void setMetaTableName(String metaTableName) {
    this.metaTableName = metaTableName;
  }

  public String getNt() {
    return nt;
  }

  public void setNt(String nt) {
    this.nt = nt;
  }

  public String getPlatform() {
    return platform;
  }

  public void setPlatform(String platform) {
    this.platform = platform;
  }

  public String getAccount() {
    return account;
  }

  public void setAccount(String account) {
    this.account = account;
  }

  public String getDb() {
    return db;
  }

  public void setDb(String db) {
    this.db = db;
  }

  public String getTbl() {
    return tbl;
  }

  public void setTbl(String tbl) {
    this.tbl = tbl;
  }

  public String getSchemaInfo() {
    return schemaInfo;
  }

  public void setSchemaInfo(String schemaInfo) {
    this.schemaInfo = schemaInfo;
  }

  public String getAuthInfo() {
    return authInfo;
  }

  public void setAuthInfo(String authInfo) {
    this.authInfo = authInfo;
  }

  public MetaTableType getMetaTableType() {
    return metaTableType;
  }

  public void setMetaTableType(MetaTableType metaTableType) {
    this.metaTableType = metaTableType;
  }

  public MetaTableStatus getMetaTableStatus() {
    return metaTableStatus;
  }

  public void setMetaTableStatus(MetaTableStatus metaTableStatus) {
    this.metaTableStatus = metaTableStatus;
  }

  public String getCron() {
    return cron;
  }

  public void setCron(String cron) {
    this.cron = cron;
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

  public String getFailLog() {
    return failLog;
  }

  public void setFailLog(String failLog) {
    this.failLog = failLog;
  }

  public String getHadoopSchemaInfo() {
    return hadoopSchemaInfo;
  }

  public void setHadoopSchemaInfo(String hadoopSchemaInfo) {
    this.hadoopSchemaInfo = hadoopSchemaInfo;
  }

  public Date getSyncTime() {
    return syncTime;
  }

  public void setSyncTime(Date syncTime) {
    this.syncTime = syncTime;
  }

  public String getLastModifier() {
    return lastModifier;
  }

  public void setLastModifier(String lastModifier) {
    this.lastModifier = lastModifier;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }
}
