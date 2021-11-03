package com.ebay.dss.zds.model;

import com.ebay.dss.zds.service.MailService;

import java.util.List;

public class ZetaEmailTemplate {

    private String jobId;

    private String nT;

    private List<String> ccList;

    private String adjunct;

    private MailService.MailTemplate mailTemplate;

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getnT() {
        return nT;
    }

    public void setnT(String nT) {
        this.nT = nT;
    }

    public MailService.MailTemplate getMailTemplate() {
        return mailTemplate;
    }

    public void setMailTemplate(MailService.MailTemplate mailTemplate) {
        this.mailTemplate = mailTemplate;
    }

    public List<String> getCcList() {
        return ccList;
    }

    public void setCcList(List<String> ccList) {
        this.ccList = ccList;
    }

    public String getAdjunct() {
        return adjunct;
    }

    public void setAdjunct(String adjunct) {
        this.adjunct = adjunct;
    }
}
