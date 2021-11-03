package com.ebay.dss.zds.model;


public class ZetaToolKitsJobEmailTemplate extends ZetaEmailTemplate {


  private String jobName;

  private String url;

  private TemplateStatus templateStatus;

  public String getJobName() {
    return jobName;
  }

  public void setJobName(String jobName) {
    this.jobName = jobName;
  }

  public TemplateStatus getTemplateStatus() {
    return templateStatus;
  }

  public void setTemplateStatus(TemplateStatus templateStatus) {
    this.templateStatus = templateStatus;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public enum TemplateStatus {
    Succeed("Succeed"),
    Failed("Failed"),
    Started("Started"),
    Canceled("Canceled"),
    Retrying("Retrying"),
    RETRY_SUCCESS("Retried Successfully"),
    RETRY_FAIL("Retried Failed");

    String desc;

    TemplateStatus(String desc) {
      this.desc = desc;
    }

    public String getDesc() {
      return desc;
    }
  }

}
