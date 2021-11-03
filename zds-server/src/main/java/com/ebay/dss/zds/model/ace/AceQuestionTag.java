package com.ebay.dss.zds.model.ace;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.persistence.*;
import java.io.Serializable;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Entity
@Table(name = "zeta_ace_question_tag")
@IdClass(AceQuestionTag.AceQuestionTagId.class)
public class AceQuestionTag {

    @Id
    private Integer questionId;
    @Id
    private Integer tagId;
    private ZonedDateTime createTime;

    public AceQuestionTag() {
    }

    public AceQuestionTag(Integer questionId, Integer tagId) {
        this.questionId = questionId;
        this.tagId = tagId;
        this.createTime = ZonedDateTime.now(ZoneId.of("UTC"));
    }

    public ZonedDateTime getCreateTime() {
        return createTime;
    }

    public AceQuestionTag setCreateTime(ZonedDateTime createTime) {
        this.createTime = createTime;
        return this;
    }

    public Integer getQuestionId() {
        return questionId;
    }

    public AceQuestionTag setQuestionId(Integer questionId) {
        this.questionId = questionId;
        return this;
    }

    public Integer getTagId() {
        return tagId;
    }

    public AceQuestionTag setTagId(Integer tagId) {
        this.tagId = tagId;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof AceQuestionTag)) return false;

        AceQuestionTag that = (AceQuestionTag) o;

        return new EqualsBuilder()
                .append(questionId, that.questionId)
                .append(tagId, that.tagId)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(questionId)
                .append(tagId)
                .toHashCode();
    }

    public static class AceQuestionTagId implements Serializable {

        private Integer questionId;
        private Integer tagId;

        public Integer getQuestionId() {
            return questionId;
        }

        public AceQuestionTagId setQuestionId(Integer questionId) {
            this.questionId = questionId;
            return this;
        }

        public Integer getTagId() {
            return tagId;
        }

        public AceQuestionTagId setTagId(Integer tagId) {
            this.tagId = tagId;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;

            if (!(o instanceof AceQuestionTagId)) return false;

            AceQuestionTagId that = (AceQuestionTagId) o;

            return new EqualsBuilder()
                    .append(questionId, that.questionId)
                    .append(tagId, that.tagId)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37)
                    .append(questionId)
                    .append(tagId)
                    .toHashCode();
        }
    }
}
