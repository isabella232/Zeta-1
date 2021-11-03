package com.ebay.dss.zds.model.ace;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.time.ZonedDateTime;

@Entity
@Table(name = "zeta_ace_tag")
public class AceTag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private String nt;
    @Column(unique = true)
    @AceTagName
    private String name;
    @Column
    @Size(max = 2048)
    private String description;
    @Column
    @AceDate
    private ZonedDateTime createTime;
    @Column
    @AceDate
    private ZonedDateTime updateTime;

    public ZonedDateTime getUpdateTime() {
        return updateTime;
    }

    public AceTag setUpdateTime(ZonedDateTime updateTime) {
        this.updateTime = updateTime;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public AceTag setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getNt() {
        return nt;
    }

    public AceTag setNt(String nt) {
        this.nt = nt;
        return this;
    }

    public Integer getId() {
        return id;
    }

    public AceTag setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public AceTag setName(String name) {
        this.name = name;
        return this;
    }

    public ZonedDateTime getCreateTime() {
        return createTime;
    }

    public AceTag setCreateTime(ZonedDateTime createTime) {
        this.createTime = createTime;
        return this;
    }
}
