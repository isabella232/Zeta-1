package com.ebay.dss.zds.model.ace;

import javax.persistence.*;
import java.time.ZonedDateTime;
import java.util.List;

@Entity
@Table(name = "zeta_ace_question_post")
public class AceQuestionPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private Integer questionId;
    @Column
    private String poster;
    @Column
    private Integer replyTo;
    @Column
    private String comment;
    @Column
    @AceDate
    private ZonedDateTime createTime;
    @Column
    @AceDate
    private ZonedDateTime updateTime;
    @Column
    private String editor;
    @Column
    @AceDate
    private ZonedDateTime editTime;
    @Column
    private Boolean accepted;

    public Boolean getAccepted() {
        return accepted;
    }

    public AceQuestionPost setAccepted(Boolean accepted) {
        this.accepted = accepted;
        return this;
    }

    public String getEditor() {
        return editor;
    }

    public AceQuestionPost setEditor(String editor) {
        this.editor = editor;
        return this;
    }

    public ZonedDateTime getEditTime() {
        return editTime;
    }

    public AceQuestionPost setEditTime(ZonedDateTime editTime) {
        this.editTime = editTime;
        return this;
    }

    public Integer getId() {
        return id;
    }

    public AceQuestionPost setId(Integer id) {
        this.id = id;
        return this;
    }

    public Integer getQuestionId() {
        return questionId;
    }

    public AceQuestionPost setQuestionId(Integer questionId) {
        this.questionId = questionId;
        return this;
    }

    public Integer getReplyTo() {
        return replyTo;
    }

    public AceQuestionPost setReplyTo(Integer replyTo) {
        this.replyTo = replyTo;
        return this;
    }

    public String getPoster() {
        return poster;
    }

    public AceQuestionPost setPoster(String poster) {
        this.poster = poster;
        return this;
    }

    public String getComment() {
        return comment;
    }

    public AceQuestionPost setComment(String comment) {
        this.comment = comment;
        return this;
    }

    public ZonedDateTime getCreateTime() {
        return createTime;
    }

    public AceQuestionPost setCreateTime(ZonedDateTime createTime) {
        this.createTime = createTime;
        return this;
    }

    public ZonedDateTime getUpdateTime() {
        return updateTime;
    }

    public AceQuestionPost setUpdateTime(ZonedDateTime updateTime) {
        this.updateTime = updateTime;
        return this;
    }

    @Override
    public String toString() {
        return "AceQuestionPost{" +
                "id=" + id +
                ", questionId=" + questionId +
                ", poster='" + poster + '\'' +
                ", replyTo=" + replyTo +
                ", comment='" + comment + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }
}
