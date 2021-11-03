package com.ebay.dss.zds.model.ace;

import java.util.List;

public class AceQuestionsDto {

    private List<AceQuestionDto> questions;
    private Long totalSize;
    private Integer totalPage;
    private Integer size;

    public List<AceQuestionDto> getQuestions() {
        return questions;
    }

    public AceQuestionsDto setQuestions(List<AceQuestionDto> questions) {
        this.questions = questions;
        return this;
    }

    public Long getTotalSize() {
        return totalSize;
    }

    public AceQuestionsDto setTotalSize(Long totalSize) {
        this.totalSize = totalSize;
        return this;
    }

    public Integer getTotalPage() {
        return totalPage;
    }

    public AceQuestionsDto setTotalPage(Integer totalPage) {
        this.totalPage = totalPage;
        return this;
    }

    public AceQuestionsDto setSize(Integer size) {
        this.size = size;
        return this;
    }

    public Integer getSize() {
        return size;
    }
}
