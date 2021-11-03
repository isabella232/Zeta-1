package com.ebay.dss.zds.service.schedule;

import com.ebay.dss.zds.common.PropertiesUtil;
import com.ebay.dss.zds.exception.ScheduleException;
import com.ebay.dss.zds.model.ZetaToolKitsJobEmailTemplate;
import com.ebay.dss.zds.model.ZetaToolKitsJobEmailTemplate.TemplateStatus;
import com.ebay.dss.zds.model.schedule.ScheduleHistory;
import com.ebay.dss.zds.model.schedule.ScheduleHistory.JobRunStatus;
import com.ebay.dss.zds.model.schedule.ScheduleJob;
import com.ebay.dss.zds.service.MailService;
import com.ebay.dss.zds.service.MailService.MailTemplate;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static com.ebay.dss.zds.service.MailService.SCHEDULE_JOB_URL;
import static com.ebay.dss.zds.service.schedule.ScheduleConstant.*;
import static com.ebay.dss.zds.service.schedule.ScheduleConstant.SCHEDULE_INACTIVATE_MAIL_CONTENT;

@Service
public class ScheduleEmailService {

  private static Logger LOGGER = LoggerFactory.getLogger(ScheduleEmailService.class);

  @Autowired
  private MailService mailService;

  @Autowired
  ScheduleJobService scheduleJobService;

  private boolean isMailSwitchOn(ScheduleJob scheduleJob) {
    return scheduleJob.getMailSwitch() == 1;
  }

  public void sendScheduleMail(ScheduleHistory history, String... logAndStatus) {
    if (isMailSwitchOn(history.getScheduleJob())) {
      LOGGER.info("Start to send schedule email - {}", history);
      try {
        JobRunStatus jobRunStatus = ArrayUtils.isNotEmpty(logAndStatus) && logAndStatus.length > 1 ?
            JobRunStatus.valueOf(logAndStatus[1]) : history.getJobRunStatus();
        TemplateStatus templateStatus = convertJobStatusToMailStatus(jobRunStatus);
        ZetaToolKitsJobEmailTemplate zetaEmailTemplate = constructScheduleJobTemplate(
            history, MailService.MailTemplate.SCHEDULER, templateStatus, logAndStatus);
        mailService.sendZetaTemplateEmail(zetaEmailTemplate);
      } catch (Exception e) {
        LOGGER.error("Send scheduler email fail!", e);
      }
    }
  }

  @Deprecated
  public void sendScheduleAlertMail(ScheduleHistory history) {
    if (isMailSwitchOn(history.getScheduleJob())) {
      LOGGER.info("Start to send schedule alert email - {}", history);
      TemplateStatus templateStatus = convertJobStatusToMailStatus(history.getJobRunStatus());
      ZetaToolKitsJobEmailTemplate zetaEmailTemplate = constructScheduleJobTemplate(
          history, MailTemplate.SCHEDULER_ALERT, templateStatus);
      mailService.sendZetaTemplateEmail(zetaEmailTemplate);
    }
  }

  private ZetaToolKitsJobEmailTemplate constructScheduleJobTemplate(ScheduleHistory history
      , MailTemplate mailTemplate, TemplateStatus templateStatus, String... jobFailLog) {
    ScheduleJob scheduleJob = history.getScheduleJob();
    return mailService.constructToolKitsJobTemplate(
        scheduleJob.getJobName(),
        String.format(SCHEDULE_JOB_URL, scheduleJob.getId()),
        templateStatus,
        scheduleJob.getNt(),
        scheduleJob.getId().toString(),
        scheduleJobService.getScheduleJobMailAddress(scheduleJob),
        ArrayUtils.isNotEmpty(jobFailLog) ? jobFailLog[0] : null,
        mailTemplate);
  }

  private TemplateStatus convertJobStatusToMailStatus(JobRunStatus status) {
    switch (status) {
      case RUNNING:
        return TemplateStatus.Started;
      case CANCELED:
        return TemplateStatus.Canceled;
      case FAIL:
        return TemplateStatus.Failed;
      case SUCCESS:
        return TemplateStatus.Succeed;
      case AUTORETRY:
        return TemplateStatus.Retrying;
      case RETRYSUCCESS:
        return TemplateStatus.RETRY_SUCCESS;
      case RETRYFAIL:
        return TemplateStatus.RETRY_FAIL;
    }
    throw new ScheduleException(String.format("Not support job status %s", status));
  }

  public void sendScheduleCleanEmail(Map<String, List<ScheduleJob>> scheduleJobGroup) {
    LOGGER.info("Start to send schedule jobs clean email - {}", scheduleJobGroup.keySet());
    mailService.sendEmail(
        MailService.MailTemplate.TEXT,
        "Clean Schedule Job",
        "Please clean unavailable users' schedule job: " + scheduleJobGroup.keySet(),
        PropertiesUtil.getScheduleProperties("cleanjob.receiver"));

  }

  public void sendScheduleTransferEmail(String user, String content, String toAddr) {
    LOGGER.info("Start to send schedule jobs transfer email to {}", toAddr);
    String mailContent = String.format(SCHEDULE_TRANSFER_MAIL_CONTENT, user, content);
    LOGGER.info("Email Content: {}", content);
    mailService.sendEmail(MailService.MailTemplate.HTML, SCHEDULE_TRANSFER_MAIL_SUBJECT
        , mailContent, toAddr);
  }

  public void sendScheduleInactivateEmail(Long jobId, String jobName, BigInteger jobNums, String toAddr) {
    LOGGER.info("Start to send clean inactive job {} email", jobId);
    String subject = String.format(SCHEDULE_INACTIVATE_MAIL_SUBJECT, jobName);
    String jobLink = String.format(SCHEDULE_JOB_URL, jobId);
    String content = String.format(SCHEDULE_INACTIVATE_MAIL_CONTENT, jobName, jobNums, jobLink);
    LOGGER.info("Email Subject: {}", subject);
    LOGGER.info("Email Content: {}", content);
    mailService.sendEmail(MailService.MailTemplate.HTML, subject, content, toAddr);
  }

  public void sendScheduleSuspendEmail(Long jobId, String jobName, int failTimes, String toAddr) {
    LOGGER.info("Send suspend schedule job {} email", jobId);
    String subject = String.format(SCHEDULE_SUSPEND_MAIL_SUBJECT, jobName);
    String jobLink = String.format(SCHEDULE_JOB_URL, jobId);
    String content = String.format(SCHEDULE_SUSPEND_MAIL_CONTENT, jobName, failTimes, jobLink);
    LOGGER.info("Email Subject: {}", subject);
    LOGGER.info("Email Content: {}", content);
    mailService.sendEmail(MailService.MailTemplate.HTML, subject, content, toAddr);
  }

}
