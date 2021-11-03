package com.ebay.dss.zds.model;

import java.sql.Timestamp;

public class ZetaStatement {

    private long id;
    private long requestId;
    private String statement;
    private int seq;
    private String result;
    //to support plotting feature
    private String plotConfig;
    private Timestamp createDt;
    private Timestamp startDt;
    private Timestamp updateDt;
    private String status;
    private int livySessionId;
    private int livyStatementId;
    private int progress;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getStatement() {
        return statement;
    }

    public void setStatement(String statement) {
        this.statement = statement;
    }

    public Timestamp getStartDt() {
        return startDt;
    }

    public void setStartDt(Timestamp startDt) {
        this.startDt = startDt;
    }

    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public int getSeq() {
        return seq;
    }

    public void setSeq(int seq) {
        this.seq = seq;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public Timestamp getCreateDt() {
        return createDt;
    }

    public void setCreateDt(Timestamp createDt) {
        this.createDt = createDt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getLivySessionId() {
        return livySessionId;
    }

    public void setLivySessionId(int livySessionId) {
        this.livySessionId = livySessionId;
    }

    public int getLivyStatementId() {
        return livyStatementId;
    }

    public void setLivyStatementId(int livyStatementId) {
        this.livyStatementId = livyStatementId;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public Timestamp getUpdateDt() {
        return updateDt;
    }

    public void setUpdateDt(Timestamp updateDt) {
        this.updateDt = updateDt;
    }

    public String getPlotConfig() {
      return plotConfig;
    }

    public void setPlotConfig(String plotConfig) {
      this.plotConfig = plotConfig;
    }
}
