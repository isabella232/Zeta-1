package com.ebay.dss.zds.model.ace;

import javax.persistence.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "zeta_ace_question_like")
public class AceQuestionLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column
    private String nt;
    @Column
    private Integer questionId;
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

    public AceQuestionLike setId(Long id) {
        this.id = id;
        return this;
    }

    public String getNt() {
        return nt;
    }

    public AceQuestionLike setNt(String nt) {
        this.nt = nt;
        return this;
    }

    public Integer getQuestionId() {
        return questionId;
    }

    public AceQuestionLike setQuestionId(Integer questionId) {
        this.questionId = questionId;
        return this;
    }

    public Integer getFlag() {
        return flag;
    }

    public AceQuestionLike setFlag(Integer flag) {
        this.flag = flag;
        return this;
    }

    public ZonedDateTime getCreateTime() {
        return createTime;
    }

    public AceQuestionLike setCreateTime(ZonedDateTime createTime) {
        this.createTime = createTime;
        return this;
    }

    public ZonedDateTime getUpdateTime() {
        return updateTime;
    }

    public AceQuestionLike setUpdateTime(ZonedDateTime updateTime) {
        this.updateTime = updateTime;
        return this;
    }
}
