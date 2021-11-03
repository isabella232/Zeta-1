package com.ebay.dss.zds.model.ace;

import java.util.List;

public class AceTagPage {

    private List<AceTag> tags;
    private int totalPage;
    private long totalSize;

    public List<AceTag> getTags() {
        return tags;
    }

    public AceTagPage setTags(List<AceTag> tags) {
        this.tags = tags;
        return this;
    }

    public int getTotalPage() {
        return totalPage;
    }

    public AceTagPage setTotalPage(int totalPage) {
        this.totalPage = totalPage;
        return this;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public AceTagPage setTotalSize(long totalSize) {
        this.totalSize = totalSize;
        return this;
    }
}
