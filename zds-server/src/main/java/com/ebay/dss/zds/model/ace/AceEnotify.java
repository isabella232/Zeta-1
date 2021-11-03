package com.ebay.dss.zds.model.ace;


import com.fasterxml.jackson.annotation.JsonUnwrapped;

public class AceEnotify {

    @JsonUnwrapped
    private DoeEnotify doeEnotify;
    @JsonUnwrapped
    private AceEnotifyRead enotifyRead;

    public DoeEnotify getDoeEnotify() {
        return doeEnotify;
    }

    public AceEnotify setDoeEnotify(DoeEnotify doeEnotify) {
        this.doeEnotify = doeEnotify;
        return this;
    }

    public AceEnotifyRead getEnotifyRead() {
        return enotifyRead;
    }

    public AceEnotify setEnotifyRead(AceEnotifyRead enotifyRead) {
        this.enotifyRead = enotifyRead;
        return this;
    }
}
