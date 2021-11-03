package com.ebay.dss.zds.websocket.notebook.dto;

import java.io.Serializable;

/**
 * Created by tatian on 2018/6/14.
 */
public class ExecuteCodeStart implements Serializable {
    private String jobId;
    private Long startDt;
    private int seq;
    private String noteId;
    private long zetaStatementKey;
    public ExecuteCodeStart(String jobId, String noteId, int seq, Long startDt, long zetaStatementKey){
        this.jobId=jobId;
        this.noteId=noteId;
        this.seq=seq;
        this.startDt=startDt;
        this.zetaStatementKey = zetaStatementKey;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public Long getStartDt() {
        return startDt;
    }

    public void setStartDt(Long startDt) {
        this.startDt = startDt;
    }

    public int getSeq() {
        return seq;
    }

    public void setSeq(int seq) {
        this.seq = seq;
    }

    public String getNoteId() {
        return noteId;
    }

    public void setNoteId(String noteId) {
        this.noteId = noteId;
    }

    public long getZetaStatementKey() {
        return zetaStatementKey;
    }

    public void setZetaStatementKey(long zetaStatementKey) {
        this.zetaStatementKey = zetaStatementKey;
    }
}
