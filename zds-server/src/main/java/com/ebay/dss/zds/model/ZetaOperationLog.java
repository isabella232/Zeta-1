package com.ebay.dss.zds.model;


import javax.persistence.*;
import javax.persistence.Column;
import javax.persistence.Table;
import java.util.Date;

@Entity
@Table(name = "zeta_operation_log")
public class ZetaOperationLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  String operationId;

  @Column(nullable = false)
  String nt;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  OperationType operationType;

  String operationInterface;

  String description;

  String comments;

  Date createTime;

  public ZetaOperationLog(String operationId, String nt) {
    this.operationId = operationId;
    this.nt = nt;
  }

  public ZetaOperationLog() {

  }

  public ZetaOperationLog(String operationId, String nt, String description, String comments) {
    this.operationId = operationId;
    this.nt = nt;
    this.description = description;
    this.comments = comments;
  }

  public enum OperationType {
    SCHEDULER
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getOperationId() {
    return operationId;
  }

  public void setOperationId(String operationId) {
    this.operationId = operationId;
  }

  public String getNt() {
    return nt;
  }

  public void setNt(String nt) {
    this.nt = nt;
  }

  public OperationType getOperationType() {
    return operationType;
  }

  public void setOperationType(OperationType operationType) {
    this.operationType = operationType;
  }

  public String getOperationInterface() {
    return operationInterface;
  }

  public void setOperationInterface(String operationInterface) {
    this.operationInterface = operationInterface;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getComments() {
    return comments;
  }

  public void setComments(String comments) {
    this.comments = comments;
  }

  public Date getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }
}
