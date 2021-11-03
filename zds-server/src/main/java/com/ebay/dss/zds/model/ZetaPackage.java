package com.ebay.dss.zds.model;


import com.ebay.dss.zds.common.JsonUtil;

import javax.persistence.*;
import javax.persistence.Table;
import java.util.Date;

@Entity
@Table(name = "zeta_package")
@Deprecated
public class ZetaPackage {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String nt;

  private String fileName;

  private String filePath;

  private int cluster;

  private String type;

  private Date createTime;

  private Date updateTime;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getNt() {
    return nt;
  }

  public void setNt(String nt) {
    this.nt = nt;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getFilePath() {
    return filePath;
  }

  public void setFilePath(String filePath) {
    this.filePath = filePath;
  }

  public int getCluster() {
    return cluster;
  }

  public void setCluster(int cluster) {
    this.cluster = cluster;
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

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String toJson() {
    return JsonUtil.GSON.toJson(this);
  }
}
