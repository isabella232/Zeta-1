package com.ebay.dss.zds.service.schedule;

import com.ebay.dss.zds.service.MailService;
import com.google.common.collect.Lists;

import java.util.List;

public class ScheduleConstant {

  public static final String SYSTEM_ACCOUNT = "SYS";
  public static final String SYS_START = "START";
  public static final String SYS_RETRY = "RETRY";
  public static final String SYS_RETRY_SUCCESS = "RETRY_SUCCESS";
  public static final String SYS_RETRY_FAIL = "RETRY_FAIL";
  static final String SCHEDULE_JOB_NOT_EXIST = "ScheduleJob %s is not exist";
  static final String SCHEDULE_JOB_INSTANCE_NOT_EXIST = "Run ID %s is not exist";
  static final String SCHEDULE_JOB_NAME_EXIST = "ScheduleJob name [%s] is exist";
  static final String SCHEDULE_JOB_RUNNING_DENIED =
      "Run ID %s is still RUNNING. Please wait for completion or you can cancel it manually.";
  static final String OPERATION_DENIED = "Not support operation [%s]";
  static final String SCHEDULE_JOB_CANCELED = "Job is canceled";
  static final String DEPENDENCY_TABLE_SIGNAL_SEND_FAILED =
      "Send dependency table signal failed. Please contact zeta team to handle. Thanks!";
  static final String SCHEDULER_NOTE_NOT_EXIST = "Notebook is not exist";
  static final String SCHEDULER_NOTE_EMPTY = "Notebook is empty";


  static final List<String> UPDATE_JOB_FIELDS = Lists.newArrayList("jobName", "scheduleTime",
      "task", "dependency", "status", "ccAddr", "mailSwitch", "dependencySignal", "failTimesToBlock", "autoRetry");

  static final String SCHEDULE_TRANSFER_MAIL_SUBJECT = "[Notification] -- Zeta schedule job owner transfer";
  static final String SCHEDULE_TRANSFER_MAIL_CONTENT = "<font size =\"3\" face=\"Calibri\" >Hi Manager,<br><br>" +
      "%s continues to have scheduled jobs running in Zeta though it appears as though they " +
      "have left the company or no longer have database access. " +
      "The schedule job owner will be transferred under your name.<br>" +
      "Names of the scheduled job currently running:<br>" +
      "<ul><li>%s</li></ul>" +
      "Click <a href=\"" + MailService.SCHEDULE_URL + "\">here</a> to manage your scheduled job.</font>";

  static final String SCHEUDL_FAIL_SQL_MAIL_CONTENT = "<div style=\"font-weight:bold; line-height:50px\">SQL</div>\n" +
      "<div style=\"width: 770px; height: 100px; overflow-y: scroll; padding:10px 10px;border-style:ridge;\">%s</div>";
  static final String SCHEUDL_FAIL_LOG_MAIL_CONTENT = "<div style=\"font-weight:bold; line-height:50px\">Error</div>\n" +
      "<div style=\"width: 770px; height: 100px; overflow-y: scroll; padding:10px 10px;border-style:ridge;\">%s</div>";

  static final String SCHEDULE_INACTIVATE_MAIL_SUBJECT = "[Notification] -- Zeta schedule job [%s] is frozen";
  static final String SCHEDULE_INACTIVATE_MAIL_CONTENT = "<div>Hi Admin,</div><br> <div>The schedule task [%s] have over %s jobs in waiting status. So we inactivate the task and" +
      " Please <a href=\"%s\">click</a> here to handle these jobs and then activate the job.</div>";

  public static final String SCHEDULE_SUSPEND_MAIL_SUBJECT = "[Notification] -- Zeta schedule job [%s] is suspend";
  public static final String SCHEDULE_SUSPEND_MAIL_CONTENT = "<div>Hi Admin,</div><br> <div>The schedule job [%s] have %s jobs in fail status which over the number you set to auto inactivate. So we suspend the task and" +
      " Please <a href=\"%s\">click</a> here to handle these job instances and then activate the job.</div>";

  static final String SCHEDULE_TRIGGER_NAME = "scheduler";
  static final String SCHEDULE_LIVY_YARN_TAGS = "zds.livy.spark.yarn.tags";
  static final String SCHEDULE_TAGS_DEFAULT = "zeta-scheduler";
  static final String SCHEDULE_HERMES_TAGS_DEFAULT = "zeta_schedule";
  static final String SCHEDULE_LIVY_JOB_NAME = "zds.livy.spark.schedule.job.name";
  static final String SCHEDULE_LIVY_JOB_ID = "zds.livy.spark.schedule.job.id";
  static final String SCHEDULE_LIVY_INSTANCE_ID = "zds.livy.spark.schedule.instance.id";

  static final String SCHEDULE_YARN_TAG_PREFIX_JOB_NAME = "zeta-scheduler-job-name-";
  static final String SCHEDULE_YARN_TAG_PREFIX_JOB_ID = "zeta-scheduler-job-id-";

  public static final String SCHEDULE_AUTO_RETRY_FAIL_OTHER_ERROR = "The job failed root cause is not hermes downtime. Please check the job failed log in 'View Result'";
  public static final String SCHEDULE_AUTO_RETRY_FAIL_HERMES_DOWN = "Hermes still can't be connected after waiting for 25 min";

}
