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
@Table(name = "data_validate_detail")
public class DataValidateDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_filter")
    private String sourceFilter;

    @Column(name = "target_filter")
    private String targetFilter;

    @Column(name = "source_batch_account")
    private String sourceBatchAccount;

    @Column(name = "target_batch_account")
    private String targetBatchAccount;

    @Column(name = "task_id")
    private int taskId;

    private String result;

    @Column(name = "cre_date")
    private Date createDate;

    @Column(name = "query")
    private String query;

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
     * @return the sourceBatchAccount
     */
    public String getSourceBatchAccount() {
        return sourceBatchAccount;
    }

    /**
     * @param sourceBatchAccount the sourceBatchAccount to set
     */
    public void setSourceBatchAccount(String sourceBatchAccount) {
        this.sourceBatchAccount = sourceBatchAccount;
    }

    /**
     * @return the targetBatchAccount
     */
    public String getTargetBatchAccount() {
        return targetBatchAccount;
    }

    /**
     * @param targetBatchAccount the targetBatchAccount to set
     */
    public void setTargetBatchAccount(String targetBatchAccount) {
        this.targetBatchAccount = targetBatchAccount;
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
     * @return the result
     */
    public String getResult() {
        return result;
    }

    /**
     * @param result the result to set
     */
    public void setResult(String result) {
        this.result = result;
    }

    /**
     * @return the sourceFilter
     */
    public String getSourceFilter() {
        return sourceFilter;
    }

    /**
     * @param sourceFilter the sourceFilter to set
     */
    public void setSourceFilter(String sourceFilter) {
        this.sourceFilter = sourceFilter;
    }

    /**
     * @return the targetFilter
     */
    public String getTargetFilter() {
        return targetFilter;
    }

    /**
     * @param targetFilter the targetFilter to set
     */
    public void setTargetFilter(String targetFilter) {
        this.targetFilter = targetFilter;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}
