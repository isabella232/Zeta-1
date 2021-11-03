package com.ebay.dss.zds.model.alation;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

@Entity
@Table(name = "alation_integrate_meta")
public class AlationQueryMeta {
  public AlationQueryMeta(int id, String author) {
    this.id = id;
    this.author = author;
    this.createDate = new Date();
    this.status = 0;
  }

  public AlationQueryMeta(int id, String author, int status) {
    this.id = id;
    this.author = author;
    this.createDate = new Date();
    this.status = status;
  }

  public AlationQueryMeta() {
  }

  @Id
  @Column(name = "alation_query_id")
  private int id;

  @Column
  private String author;

  @Column(name = "crt_dt")
  private Date createDate;

  @Column(name = "crt_by")
  private String creator;

  @Column(name = "upd_dt")
  private Date updateDate;

  @Column
  private int status;

  @Column(name = "schedule_status")
  private int scheduleStatus;
  
  @Column(name = "published_status")
  private int publishedStatus;

  
  public int getPublishedStatus() {
	return publishedStatus;
}

public void setPublishedStatus(int publishedStatus) {
	this.publishedStatus = publishedStatus;
}

public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public Date getCreateDate() {
    return createDate;
  }

  public void setCreateDate(Date createDate) {
    this.createDate = createDate;
  }

  public Date getUpdateDate() {
    return updateDate;
  }

  public void setUpdateDate(Date updateDate) {
    this.updateDate = updateDate;
  }

  public String getCreator() {
    return creator;
  }

  public void setCreator(String creator) {
    this.creator = creator;
  }

  public int getStatus() {
    return status;
  }

  public void setStatus(int status) {
    this.status = status;
  }

  public int getScheduleStatus() {
    return scheduleStatus;
  }

  public void setScheduleStatus(int scheduleStatus) {
    this.scheduleStatus = scheduleStatus;
  }
}
