package com.ebay.dss.zds.model.ace;

import javax.persistence.Column;
import javax.persistence.Table;
import javax.persistence.*;
import java.time.ZonedDateTime;
import java.util.List;

@Entity
@Table(name = "zeta_ace_question")
public class AceQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private String title;
    @Column
    private String content;
    @Column
    private String submitter;
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
    private Integer pickUp;

    public Integer getPickUp() {
        return pickUp;
    }

    public AceQuestion setPickUp(Integer pickUp) {
        this.pickUp = pickUp;
        return this;
    }

    public String getEditor() {
        return editor;
    }

    public AceQuestion setEditor(String editor) {
        this.editor = editor;
        return this;
    }

    public ZonedDateTime getEditTime() {
        return editTime;
    }

    public AceQuestion setEditTime(ZonedDateTime editTime) {
        this.editTime = editTime;
        return this;
    }

    public String getContent() {
        return content;
    }

    public AceQuestion setContent(String content) {
        this.content = content;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public AceQuestion setTitle(String title) {
        this.title = title;
        return this;
    }

    public Integer getId() {
        return id;
    }

    public AceQuestion setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getSubmitter() {
        return submitter;
    }

    public AceQuestion setSubmitter(String submitter) {
        this.submitter = submitter;
        return this;
    }

    public ZonedDateTime getCreateTime() {
        return createTime;
    }

    public AceQuestion setCreateTime(ZonedDateTime createTime) {
        this.createTime = createTime;
        return this;
    }

    public ZonedDateTime getUpdateTime() {
        return updateTime;
    }

    public AceQuestion setUpdateTime(ZonedDateTime updateTime) {
        this.updateTime = updateTime;
        return this;
    }

    @Override
    public String toString() {
        return "AceQuestion{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", submitter='" + submitter + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }
}
