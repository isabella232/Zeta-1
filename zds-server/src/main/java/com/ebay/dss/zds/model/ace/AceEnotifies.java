package com.ebay.dss.zds.model.ace;

import java.util.List;

public class AceEnotifies {

    private List<AceEnotify> enotifies;

    public List<AceEnotify> getEnotifies() {
        return enotifies;
    }

    public AceEnotifies setEnotifies(List<AceEnotify> enotifies) {
        this.enotifies = enotifies;
        return this;
    }
}
