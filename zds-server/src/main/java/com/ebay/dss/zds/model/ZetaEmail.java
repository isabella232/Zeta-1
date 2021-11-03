package com.ebay.dss.zds.model;

import javax.persistence.*;
import javax.persistence.Column;
import java.util.Date;

@Entity
public class ZetaEmail {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String jobId;

  private Long emailId;

  @Column(nullable = false)
  private String status;

  private String errorLog;

  @Column(nullable = false)
  private Date createTime;

  private String template;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getJobId() {
    return jobId;
  }

  public void setJobId(String jobId) {
    this.jobId = jobId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Date getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }

  public Long getEmailId() {
    return emailId;
  }

  public void setEmailId(Long emailId) {
    this.emailId = emailId;
  }

  public String getErrorLog() {
    return errorLog;
  }

  public void setErrorLog(String errorLog) {
    this.errorLog = errorLog;
  }

  public String getTemplate() {
    return template;
  }

  public void setTemplate(String template) {
    this.template = template;
  }
}
