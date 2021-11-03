package com.ebay.dss.zds.model.ace;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.persistence.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "zeta_ace_enotify_read")
@IdClass(AceEnotifyReadId.class)
public class AceEnotifyRead {

    @Id
    @Column
    @JsonIgnore
    private Long enotifyId;

    @Id
    @Column
    @JsonIgnore
    private String nt;
    @Column(name = "\"read\"")
    private boolean read;
    @Column
    @AceDate
    private ZonedDateTime readTime;

    public Long getEnotifyId() {
        return enotifyId;
    }

    public AceEnotifyRead setEnotifyId(Long enotifyId) {
        this.enotifyId = enotifyId;
        return this;
    }

    public String getNt() {
        return nt;
    }

    public AceEnotifyRead setNt(String nt) {
        this.nt = nt;
        return this;
    }

    public boolean isRead() {
        return read;
    }

    public AceEnotifyRead setRead(boolean read) {
        this.read = read;
        return this;
    }

    public ZonedDateTime getReadTime() {
        return readTime;
    }

    public AceEnotifyRead setReadTime(ZonedDateTime readTime) {
        this.readTime = readTime;
        return this;
    }
}
