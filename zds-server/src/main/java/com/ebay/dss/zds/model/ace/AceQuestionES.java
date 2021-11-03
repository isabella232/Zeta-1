package com.ebay.dss.zds.model.ace;

import java.time.ZonedDateTime;
import java.util.Objects;

public class AceQuestionES {

    private Integer questionId;
    private String title;
    private Integer postId;
    private String submitter;
    private String comment;
    private Integer likeCnt;
    private Integer postCnt;
    @AceDate
    private ZonedDateTime createTime;
    @AceDate
    private ZonedDateTime updateTime;

    public ZonedDateTime getCreateTime() {
        return createTime;
    }

    public AceQuestionES setCreateTime(ZonedDateTime createTime) {
        this.createTime = createTime;
        return this;
    }

    public ZonedDateTime getUpdateTime() {
        return updateTime;
    }

    public AceQuestionES setUpdateTime(ZonedDateTime updateTime) {
        this.updateTime = updateTime;
        return this;
    }

    public String getSubmitter() {
        return submitter;
    }

    public AceQuestionES setSubmitter(String submitter) {
        this.submitter = submitter;
        return this;
    }

    public Integer getLikeCnt() {
        return likeCnt;
    }

    public AceQuestionES setLikeCnt(Integer likeCnt) {
        this.likeCnt = likeCnt;
        return this;
    }

    public Integer getPostCnt() {
        return postCnt;
    }

    public AceQuestionES setPostCnt(Integer postCnt) {
        this.postCnt = postCnt;
        return this;
    }

    public Integer getPostId() {
        return postId;
    }

    public AceQuestionES setPostId(Integer postId) {
        this.postId = postId;
        return this;
    }

    public String getComment() {
        return comment;
    }

    public AceQuestionES setComment(String comment) {
        this.comment = comment;
        return this;
    }

    public Integer getQuestionId() {
        return questionId;
    }

    public AceQuestionES setQuestionId(Integer questionId) {
        this.questionId = questionId;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public AceQuestionES setTitle(String title) {
        this.title = title;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AceQuestionES that = (AceQuestionES) o;
        return questionId.equals(that.questionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(questionId);
    }

    @Override
    public String toString() {
        return "AceQuestionEntry{" +
                "questionId=" + questionId +
                ", title='" + title + '\'' +
                ", postId=" + postId +
                ", submitter='" + submitter + '\'' +
                ", comment='" + comment + '\'' +
                ", likeCnt=" + likeCnt +
                ", postCnt=" + postCnt +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }
}
