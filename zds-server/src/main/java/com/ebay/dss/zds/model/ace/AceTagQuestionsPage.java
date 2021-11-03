package com.ebay.dss.zds.model.ace;


import java.util.List;
import java.util.Set;

public class AceTagQuestionsPage {

    private Set<Integer> tagIds;
    private List<Integer> questionIds;
    private int totalPage;
    private long totalSize;

    public Set<Integer> getTagIds() {
        return tagIds;
    }

    public AceTagQuestionsPage setTagIds(Set<Integer> tagIds) {
        this.tagIds = tagIds;
        return this;
    }

    public List<Integer> getQuestionIds() {
        return questionIds;
    }

    public AceTagQuestionsPage setQuestionIds(List<Integer> questionIds) {
        this.questionIds = questionIds;
        return this;
    }

    public int getTotalPage() {
        return totalPage;
    }

    public AceTagQuestionsPage setTotalPage(int totalPage) {
        this.totalPage = totalPage;
        return this;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public AceTagQuestionsPage setTotalSize(long totalSize) {
        this.totalSize = totalSize;
        return this;
    }
}
