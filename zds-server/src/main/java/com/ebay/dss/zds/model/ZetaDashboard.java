package com.ebay.dss.zds.model;


import java.sql.Timestamp;

public class ZetaDashboard {
  private long id;
  private String nt;
  private String name;
  private String path;
  private String layoutConfig;
  private Timestamp createDt;
  private Timestamp updateDt;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getNt() {
    return nt;
  }

  public void setNt(String nt) {
    this.nt = nt;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getLayoutConfig() {
    return layoutConfig;
  }

  public void setLayoutConfig(String layoutConfig) {
    this.layoutConfig = layoutConfig;
  }

  public Timestamp getCreateDt() {
    return createDt;
  }

  public void setCreateDt(Timestamp createDt) {
    this.createDt = createDt;
  }

  public Timestamp getUpdateDt() {
    return updateDt;
  }

  public void setUpdateDt(Timestamp updateDt) {
    this.updateDt = updateDt;
  }
}
