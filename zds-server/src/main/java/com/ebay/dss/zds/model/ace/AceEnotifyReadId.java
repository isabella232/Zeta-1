package com.ebay.dss.zds.model.ace;


import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;

public class AceEnotifyReadId implements Serializable {

    private Long enotifyId;
    private String nt;

    public Long getEnotifyId() {
        return enotifyId;
    }

    public AceEnotifyReadId setEnotifyId(Long enotifyId) {
        this.enotifyId = enotifyId;
        return this;
    }

    public String getNt() {
        return nt;
    }

    public AceEnotifyReadId setNt(String nt) {
        this.nt = nt;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof AceEnotifyReadId)) return false;

        AceEnotifyReadId that = (AceEnotifyReadId) o;

        return new EqualsBuilder()
                .append(enotifyId, that.enotifyId)
                .append(nt, that.nt)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(enotifyId)
                .append(nt)
                .toHashCode();
    }
}
