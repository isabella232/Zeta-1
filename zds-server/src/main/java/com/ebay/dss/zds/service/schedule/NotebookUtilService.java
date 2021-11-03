package com.ebay.dss.zds.service.schedule;

import com.ebay.dss.zds.common.JsonUtil;
import com.ebay.dss.zds.common.PropertiesUtil;
import com.ebay.dss.zds.exception.ScheduleException;
import com.ebay.dss.zds.interpreter.ConfigurationManager;
import com.ebay.dss.zds.interpreter.api.InterpreterService;
import com.ebay.dss.zds.interpreter.interpreters.Interpreter;
import com.ebay.dss.zds.model.NoteBookSchedulerJob;
import com.ebay.dss.zds.model.schedule.ScheduleHistory;
import com.ebay.dss.zds.model.schedule.ScheduleJob;
import com.ebay.dss.zds.service.MonitorService;
import com.ebay.dss.zds.websocket.notebook.dto.ExecuteCodeReq;
import com.ebay.dss.zds.interpreter.ConfigurationManager.YarnTags;
import com.google.common.base.Splitter;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static com.ebay.dss.zds.common.Constant.ZDS_OPERATION_TRIGGER;
import static com.ebay.dss.zds.interpreter.InterpreterType.ConfType.ZDS_JDBC;
import static com.ebay.dss.zds.service.schedule.ScheduleConstant.*;

@Service
public class NotebookUtilService {

  private Logger LOGGER = LoggerFactory.getLogger(NotebookUtilService.class);

  @Autowired
  InterpreterService interpreterService;

  @Autowired
  private ConfigurationManager confManager;

  @Autowired
  MonitorService monitorService;

  private static Cache<Long, String> HERMES_JOB_INSTANCE_INFO = CacheBuilder.newBuilder()
      .maximumSize(5000)
      .expireAfterWrite(2, TimeUnit.DAYS)
      .expireAfterAccess(1, TimeUnit.HOURS)
      .build();


  @Retryable(maxAttempts = 5, value = Exception.class, backoff = @Backoff(delay = 300000))
  public Interpreter openInterpreter(String nt,
                                     String noteId,
                                     String className,
                                     Properties properties) {
    LOGGER.info("Open Interpreter NT-{}, NoteId-{}, ClassName-{}, Properties-{}"
        , nt, noteId, className, properties);
    return interpreterService.openInterpreter(nt, noteId, className, properties);
  }

  public Properties getInterpreterProperties(String nt,
                                             NoteBookSchedulerJob noteBookSchedulerJob,
                                             ExecuteCodeReq req,
                                             String noteId,
                                             String className) {
    Properties properties;
    LOGGER.info("Scheduler Job NoteId-{} ProxyUser-{}, Cluster-{}, ClassName-{}"
        , noteId, noteBookSchedulerJob.getProxyUser(), noteBookSchedulerJob.getClusterId(), className);
    Map<String, String> prop = Optional.ofNullable(noteBookSchedulerJob.getProp())
        .orElse(Maps.newLinkedHashMap());
    prop.put("clusterId", Integer.toString(noteBookSchedulerJob.getClusterId()));
    prop.put("proxyUser", noteBookSchedulerJob.getProxyUser());
    if (!prop.containsKey("preference")) {
      prop.put("preference", "");
    }
    properties = confManager.prepareConfiguration(nt, noteId, req.getInterpreter(), prop).getProperties();
    //for livy
    String interval = PropertiesUtil.getScheduleProperties("livy.interval");
    LOGGER.info("Set schedule Interval value as [{}ms]", interval);
    properties.setProperty("zds.livy.pull_status.interval.millis", interval);
    return properties;
  }

  public Properties setSchedulerInfo(Properties properties, ScheduleHistory scheduleHistory) {
    ScheduleJob scheduleJob = scheduleHistory.getScheduleJob();
    properties.setProperty(ZDS_OPERATION_TRIGGER, SCHEDULE_TRIGGER_NAME);
    if (!properties.containsKey(SCHEDULE_LIVY_YARN_TAGS)) {
      properties.setProperty(SCHEDULE_LIVY_YARN_TAGS, SCHEDULE_TAGS_DEFAULT);
    } else {
      String tags = properties.getProperty(SCHEDULE_LIVY_YARN_TAGS);
      if (!tags.contains(SCHEDULE_TAGS_DEFAULT)) {
        String merged = YarnTags.add(tags, SCHEDULE_TAGS_DEFAULT);
        properties.setProperty(SCHEDULE_LIVY_YARN_TAGS, merged);
      }
    }
    // also add scheduler tags in the yarn tags
    String yarnTags = properties.getProperty(SCHEDULE_LIVY_YARN_TAGS);
    yarnTags = YarnTags.add(yarnTags, SCHEDULE_YARN_TAG_PREFIX_JOB_NAME + scheduleJob.getJobName());
    yarnTags = YarnTags.add(yarnTags, SCHEDULE_YARN_TAG_PREFIX_JOB_ID + scheduleJob.getId());
    properties.put(SCHEDULE_LIVY_YARN_TAGS, yarnTags);

    properties.setProperty(SCHEDULE_LIVY_JOB_NAME, scheduleJob.getJobName());
    properties.setProperty(SCHEDULE_LIVY_JOB_ID, scheduleJob.getId() + "");
    properties.setProperty(SCHEDULE_LIVY_INSTANCE_ID, scheduleHistory.getId() + "");

    LOGGER.info("Set schedule context to interpreter, jobName: [{}], jobId: [{}], instanceId: [{}]",
        scheduleJob.getJobName(), scheduleJob.getId(), scheduleHistory.getId());
    return properties;
  }

}
