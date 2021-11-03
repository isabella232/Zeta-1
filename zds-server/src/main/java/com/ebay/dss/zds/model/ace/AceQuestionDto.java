package com.ebay.dss.zds.model.ace;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.ZonedDateTime;
import java.util.List;

public class AceQuestionDto {

    private Integer id;
    private String title;
    private String submitter;
    private String content;
    @AceDate
    private ZonedDateTime createTime;
    @AceDate
    private ZonedDateTime updateTime;
    private String editor;
    @AceDate
    private ZonedDateTime editTime;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<AceQuestionPostDto> posts;
    private Long totalPost;
    private Long totalLevel1Post;
    private Long totalPostLike;
    private Long totalLike;
    private Integer liked;
    private Integer pickUp;

    public Integer getPickUp() {
        return pickUp;
    }

    public AceQuestionDto setPickUp(Integer pickUp) {
        this.pickUp = pickUp;
        return this;
    }

    public String getEditor() {
        return editor;
    }

    public AceQuestionDto setEditor(String editor) {
        this.editor = editor;
        return this;
    }

    public ZonedDateTime getEditTime() {
        return editTime;
    }

    public AceQuestionDto setEditTime(ZonedDateTime editTime) {
        this.editTime = editTime;
        return this;
    }

    public Long getTotalLevel1Post() {
        return totalLevel1Post;
    }

    public AceQuestionDto setTotalLevel1Post(Long totalLevel1Post) {
        this.totalLevel1Post = totalLevel1Post;
        return this;
    }

    public String getContent() {
        return content;
    }

    public AceQuestionDto setContent(String content) {
        this.content = content;
        return this;
    }

    public Long getTotalPostLike() {
        return totalPostLike;
    }

    public AceQuestionDto setTotalPostLike(Long totalPostLike) {
        this.totalPostLike = totalPostLike;
        return this;
    }

    public Integer getLiked() {
        return liked;
    }

    public AceQuestionDto setLiked(Integer liked) {
        this.liked = liked;
        return this;
    }

    public List<AceQuestionPostDto> getPosts() {
        return posts;
    }

    public AceQuestionDto setPosts(List<AceQuestionPostDto> posts) {
        this.posts = posts;
        return this;
    }

    public Integer getId() {
        return id;
    }

    public AceQuestionDto setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public AceQuestionDto setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getSubmitter() {
        return submitter;
    }

    public AceQuestionDto setSubmitter(String submitter) {
        this.submitter = submitter;
        return this;
    }

    public ZonedDateTime getCreateTime() {
        return createTime;
    }

    public AceQuestionDto setCreateTime(ZonedDateTime createTime) {
        this.createTime = createTime;
        return this;
    }

    public ZonedDateTime getUpdateTime() {
        return updateTime;
    }

    public AceQuestionDto setUpdateTime(ZonedDateTime updateTime) {
        this.updateTime = updateTime;
        return this;
    }

    public Long getTotalPost() {
        return totalPost;
    }

    public AceQuestionDto setTotalPost(Long totalPost) {
        this.totalPost = totalPost;
        return this;
    }

    public Long getTotalLike() {
        return totalLike;
    }

    public AceQuestionDto setTotalLike(Long totalLike) {
        this.totalLike = totalLike;
        return this;
    }

    @Override
    public String toString() {
        return "AceQuestionVO{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", submitter='" + submitter + '\'' +
                ", content='" + content + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                ", posts=" + posts +
                ", totalPost=" + totalPost +
                ", totalLevel1Post=" + totalLevel1Post +
                ", totalPostLike=" + totalPostLike +
                ", totalLike=" + totalLike +
                ", liked=" + liked +
                '}';
    }
}
