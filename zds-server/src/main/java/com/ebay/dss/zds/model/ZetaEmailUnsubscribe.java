package com.ebay.dss.zds.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.Column;
import javax.persistence.*;
import java.util.Date;

@Entity
@Deprecated
public class ZetaEmailUnsubscribe {

    @Id
    @Column(nullable = false)
    private String unsubscribeId;

    @Column(nullable = false)
    @JsonProperty("nT")
    private String nt;

    @Column(nullable = false)
    private Date createTime;

    public String getUnsubscribeId() {
        return unsubscribeId;
    }

    public void setUnsubscribeId(String unsubscribeId) {
        this.unsubscribeId = unsubscribeId;
    }

    public String getNt() {
        return nt;
    }

    public void setNt(String nT) {
        this.nt = nT;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
