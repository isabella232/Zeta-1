package com.ebay.dss.zds.model.ace;

import javax.persistence.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "zeta_ace_post_like")
public class AcePostLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column
    private String nt;
    @Column
    private Integer questionId;
    @Column
    private Integer postId;
    @Column
    private Integer flag;
    @Column
    @AceDate
    private ZonedDateTime createTime;
    @Column
    @AceDate
    private ZonedDateTime updateTime;

    public Long getId() {
        return id;
    }

    public AcePostLike setId(Long id) {
        this.id = id;
        return this;
    }

    public String getNt() {
        return nt;
    }

    public AcePostLike setNt(String nt) {
        this.nt = nt;
        return this;
    }

    public Integer getQuestionId() {
        return questionId;
    }

    public AcePostLike setQuestionId(Integer questionId) {
        this.questionId = questionId;
        return this;
    }

    public Integer getPostId() {
        return postId;
    }

    public AcePostLike setPostId(Integer postId) {
        this.postId = postId;
        return this;
    }

    public Integer getFlag() {
        return flag;
    }

    public AcePostLike setFlag(Integer flag) {
        this.flag = flag;
        return this;
    }

    public ZonedDateTime getCreateTime() {
        return createTime;
    }

    public AcePostLike setCreateTime(ZonedDateTime createTime) {
        this.createTime = createTime;
        return this;
    }

    public ZonedDateTime getUpdateTime() {
        return updateTime;
    }

    public AcePostLike setUpdateTime(ZonedDateTime updateTime) {
        this.updateTime = updateTime;
        return this;
    }
}
