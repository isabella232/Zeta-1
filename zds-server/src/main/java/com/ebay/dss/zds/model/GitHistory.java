package com.ebay.dss.zds.model;

import javax.persistence.*;
import javax.persistence.Table;
import java.util.Date;

/**
 * Created by zhouhuang on 2019/3/15.
 */
@Entity
@Table(name = "git_history")
public class GitHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nT;

    private String gitUrl;

    private String branch;

    private String tag;

    private String sha;

    private String fileList;

    private Date createTime;

    public GitHistory(){}

    public GitHistory(String nT, String gitUrl, String branch, String tag, String sha, String fileList,Date createTime) {
        this.nT = nT;
        this.gitUrl = gitUrl;
        this.branch = branch;
        this.tag = tag;
        this.sha = sha;
        this.fileList = fileList;
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

    public String getGitUrl() {
        return gitUrl;
    }

    public void setGitUrl(String gitUrl) {
        this.gitUrl = gitUrl;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getFileList() {
        return fileList;
    }

    public void setFileList(String fileList) {
        this.fileList = fileList;
    }

    public String getSha() {
        return sha;
    }

    public void setSha(String sha) {
        this.sha = sha;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
