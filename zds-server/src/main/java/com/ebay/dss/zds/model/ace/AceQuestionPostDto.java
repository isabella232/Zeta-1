package com.ebay.dss.zds.model.ace;

import java.time.ZonedDateTime;

public class AceQuestionPostDto {
    private Integer id;
    private Integer questionId;
    private String poster;
    private Integer replyTo;
    private String comment;
    @AceDate
    private ZonedDateTime createTime;
    @AceDate
    private ZonedDateTime updateTime;
    private Boolean accepted;

    private Long totalLike;
    private Integer liked;
    private String editor;
    @AceDate
    private ZonedDateTime editTime;

    public Boolean getAccepted() {
        return accepted;
    }

    public AceQuestionPostDto setAccepted(Boolean accepted) {
        this.accepted = accepted;
        return this;
    }

    public String getEditor() {
        return editor;
    }

    public AceQuestionPostDto setEditor(String editor) {
        this.editor = editor;
        return this;
    }

    public ZonedDateTime getEditTime() {
        return editTime;
    }

    public AceQuestionPostDto setEditTime(ZonedDateTime editTime) {
        this.editTime = editTime;
        return this;
    }

    public Integer getId() {
        return id;
    }

    public AceQuestionPostDto setId(Integer id) {
        this.id = id;
        return this;
    }

    public Integer getQuestionId() {
        return questionId;
    }

    public AceQuestionPostDto setQuestionId(Integer questionId) {
        this.questionId = questionId;
        return this;
    }

    public String getPoster() {
        return poster;
    }

    public AceQuestionPostDto setPoster(String poster) {
        this.poster = poster;
        return this;
    }

    public Integer getReplyTo() {
        return replyTo;
    }

    public AceQuestionPostDto setReplyTo(Integer replyTo) {
        this.replyTo = replyTo;
        return this;
    }

    public String getComment() {
        return comment;
    }

    public AceQuestionPostDto setComment(String comment) {
        this.comment = comment;
        return this;
    }

    public ZonedDateTime getCreateTime() {
        return createTime;
    }

    public AceQuestionPostDto setCreateTime(ZonedDateTime createTime) {
        this.createTime = createTime;
        return this;
    }

    public ZonedDateTime getUpdateTime() {
        return updateTime;
    }

    public AceQuestionPostDto setUpdateTime(ZonedDateTime updateTime) {
        this.updateTime = updateTime;
        return this;
    }

    public Long getTotalLike() {
        return totalLike;
    }

    public AceQuestionPostDto setTotalLike(Long totalLike) {
        this.totalLike = totalLike;
        return this;
    }

    public Integer getLiked() {
        return liked;
    }

    public AceQuestionPostDto setLiked(Integer liked) {
        this.liked = liked;
        return this;
    }
}
