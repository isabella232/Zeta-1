package com.ebay.dss.zds.model.ace;

public class AceQuestionCountByTag {
    private Integer tagId;
    private Long count;

    public AceQuestionCountByTag(Integer tagId, Long count) {
        this.tagId = tagId;
        this.count = count;
    }

    public Long getCount() {
        return count;
    }

    public AceQuestionCountByTag setCount(Long count) {
        this.count = count;
        return this;
    }

    public Integer getTagId() {
        return tagId;
    }

    public AceQuestionCountByTag setTagId(Integer tagId) {
        this.tagId = tagId;
        return this;
    }
}
