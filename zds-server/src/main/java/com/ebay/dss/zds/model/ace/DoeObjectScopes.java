package com.ebay.dss.zds.model.ace;

import java.util.Set;

public class DoeObjectScopes extends DoeSearchObjectBase {

    private Set<String> value;

    public Set<String> getValue() {
        return value;
    }

    public DoeObjectScopes setValue(Set<String> value) {
        this.value = value;
        return this;
    }

}
