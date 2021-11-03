package com.ebay.dss.zds.model;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "zeta_track_log")
public class ZetaTrackLog {

  @Id
  @Column(nullable = false)
  String eventId;

  @Column(nullable = false)
  String nt;

  String action;

  String createTime;

  public String getEventId() {
    return eventId;
  }

  public void setEventId(String eventId) {
    this.eventId = eventId;
  }

  public String getNt() {
    return nt;
  }

  public void setNt(String nt) {
    this.nt = nt;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public String getCreateTime() {
    return createTime;
  }

  public void setCreateTime(String createTime) {
    this.createTime = createTime;
  }
}
