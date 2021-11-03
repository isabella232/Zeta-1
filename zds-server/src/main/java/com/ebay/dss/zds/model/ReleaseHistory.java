package com.ebay.dss.zds.model;

import javax.persistence.*;
import javax.persistence.Table;
import java.util.Date;

/**
 * Created by zhouhuang on 2019/3/15.
 */
@Entity
@Table(name = "release_history")
public class ReleaseHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nT;

    private String releaseTag;

    private String execTask;

    private Date createTime;

    public ReleaseHistory(){}

    public ReleaseHistory(String nT,String releaseTag,String execTask,Date createTime){
        this.nT = nT;
        this.releaseTag = releaseTag;
        this.execTask = execTask;
        this.createTime = createTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getnT() {
        return nT;
    }

    public void setnT(String nT) {
        this.nT = nT;
    }

    public String getReleaseTag() {
        return releaseTag;
    }

    public void setReleaseTag(String releaseTag) {
        this.releaseTag = releaseTag;
    }

    public String getExecTask() {
        return execTask;
    }

    public void setExecTask(String execTask) {
        this.execTask = execTask;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
