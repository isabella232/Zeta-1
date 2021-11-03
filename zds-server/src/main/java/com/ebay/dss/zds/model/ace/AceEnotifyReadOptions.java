package com.ebay.dss.zds.model.ace;

import javax.validation.constraints.NotEmpty;
import java.util.Set;

public class AceEnotifyReadOptions {

    private String user;
    @NotEmpty
    private Set<Long> enotifyIds;
    private boolean read = true;

    public boolean isRead() {
        return read;
    }

    public AceEnotifyReadOptions setRead(boolean read) {
        this.read = read;
        return this;
    }

    public String getUser() {
        return user;
    }

    public AceEnotifyReadOptions setUser(String user) {
        this.user = user;
        return this;
    }

    public Set<Long> getEnotifyIds() {
        return enotifyIds;
    }

    public AceEnotifyReadOptions setEnotifyIds(Set<Long> enotifyIds) {
        this.enotifyIds = enotifyIds;
        return this;
    }

}
