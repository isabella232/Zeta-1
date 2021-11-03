package com.ebay.dss.zds.model.alation;


import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;


public class AlationQueryMapPK implements Serializable {

    private String notebookId;

    private int queryId;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof AlationQueryMapPK)) return false;

        AlationQueryMapPK that = (AlationQueryMapPK) o;

        return new EqualsBuilder()
                .append(queryId, that.queryId)
                .append(notebookId, that.notebookId)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(notebookId)
                .append(queryId)
                .toHashCode();
    }
}
