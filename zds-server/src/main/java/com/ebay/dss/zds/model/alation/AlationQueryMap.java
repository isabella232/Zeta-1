package com.ebay.dss.zds.model.alation;

import javax.persistence.*;

@Entity
@IdClass(AlationQueryMapPK.class)
@Table(name = "alation_query_map")
public class AlationQueryMap {
    public AlationQueryMap(String notebookId, int queryId) {
        this.notebookId = notebookId;
        this.queryId = queryId;
    }

    public AlationQueryMap() {
    }
    @Id
    @Column
    String notebookId;

    @Id
    @Column
    int queryId;

    @Column
    Long scheduleId;

    public String getNotebookId() {
        return notebookId;
    }

    public void setNotebookId(String notebookId) {
        this.notebookId = notebookId;
    }

    public int getQueryId() {
        return queryId;
    }

    public void setQueryId(int queryId) {
        this.queryId = queryId;
    }

    public Long getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(Long scheduleId) {
        this.scheduleId = scheduleId;
    }
}