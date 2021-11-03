package com.ebay.dss.zds.service.schedule;


import com.ebay.dss.zds.common.DateUtil;
import com.ebay.dss.zds.common.JsonUtil;
import com.ebay.dss.zds.dao.HumanResourceRepository;
import com.ebay.dss.zds.dao.ScheduleHistoryRepository;
import com.ebay.dss.zds.dao.ScheduleJobRepository;
import com.ebay.dss.zds.model.schedule.ScheduleHistory;
import com.ebay.dss.zds.model.schedule.ScheduleJob;
import com.ebay.dss.zds.model.schedule.ScheduleTime;
import com.google.common.collect.Lists;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

import static com.ebay.dss.zds.model.schedule.ScheduleHistory.JobRunStatus.*;

@Component
public class SchedulerClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(SchedulerClient.class);

  @Autowired
  ScheduleJobRepository scheduleJobRepository;

  @Autowired
  ScheduleHistoryRepository scheduleHistoryRepository;

  @Autowired
  private HumanResourceRepository humanResourceRepository;

  @Autowired
  NotebookScheduler notebookScheduler;

  @Autowired
  DataMoveScheduler dataMoveScheduler;

  @Autowired
  DataValidateScheduler dataValidateScheduler;

  @Autowired
  MetaTableScheduler metaTableScheduler;


  @Resource(name = "resttemplate")
  private RestTemplate restTemplate;

  @Autowired
  private ScheduleEmailService mailService;

  private static String USER_INFO_API;

  @Value("${doe.old.url}")
  private void initServiceUrl(String hostUrl) {
//    static String USER_INFO_API = "http://opsins-service.dss-doe.svc.57.tess.io/solr/typeAhead?core=user&q=%s&type=user";
    USER_INFO_API = hostUrl + "/solr/typeAhead?core=user&q=%s&type=user";
  }


  private static final List<String> MNG_LIST = Lists.newArrayList(
      "MNGR9_NT_LOGIN",
      "MNGR8_NT_LOGIN",
      "MNGR7_NT_LOGIN",
      "MNGR6_NT_LOGIN",
      "MNGR5_NT_LOGIN",
      "MNGR4_NT_LOGIN");

  public void cleanScheduleJob() {
    LOGGER.info("Start to clean unavailable user schedule job [{}]",
        System.currentTimeMillis());
    List<ScheduleJob> scheduleJobList = scheduleJobRepository.findByStatus(1);
    if (CollectionUtils.isEmpty(scheduleJobList)) {
      return;
    }
    Map<String, List<ScheduleJob>> scheduleJobGroup = scheduleJobList.stream()
        .collect(Collectors.groupingBy(ScheduleJob::getNt));
    LOGGER.info("User Scheduled job List {}", scheduleJobGroup);
    List<String> availableNtList = humanResourceRepository
        .getUsersInfo(scheduleJobGroup.keySet());
    LOGGER.info("Available User List {}", availableNtList);
    availableNtList.stream().forEach(scheduleJobGroup::remove);
    LOGGER.info("Not Available User List {}", scheduleJobGroup);
    if (CollectionUtils.isEmpty(scheduleJobGroup)) {
      return;
    }
    mailService.sendScheduleCleanEmail(scheduleJobGroup);

    for (String nt : scheduleJobGroup.keySet()) {
      ResponseEntity<String> responseEntity = restTemplate.getForEntity(
          String.format(USER_INFO_API, nt), String.class);
      if (responseEntity.getStatusCode() == HttpStatus.OK) {
        Object document = Configuration.defaultConfiguration().jsonProvider()
            .parse(responseEntity.getBody());
        String mngNt = getMngNT(document, nt);
        String userName = getUsername(document);
        if (Objects.nonNull(mngNt)) {
          StringBuilder joblist = new StringBuilder();
          for (ScheduleJob scheduleJob : scheduleJobGroup.get(nt)) {
            joblist.append(scheduleJob.getJobName()).append("<br>");
            scheduleJob.setNt(mngNt);
            scheduleJobRepository.save(scheduleJob);
          }
          mailService.sendScheduleTransferEmail(userName, joblist.toString(),
              mngNt + "@ebay.com");
        }
      }
    }
        /*for (ScheduleJob scheduleJob : scheduleJobList) {
            if (!availableNtList.contains(scheduleJob.getNt())) {
                scheduleJob.setStatus(0);
                scheduleJobRepository.save(scheduleJob);
            }
        }*/
  }

  private String getMngNT(Object document, String nt) {
    try {
      LocalDate endDate = DateUtil.parseStringToDate(JsonPath.read(document,
          "$.response.docs[0].END_DT"), DateUtil.DT);
      if (endDate.isBefore(LocalDate.now())) {
        for (String mng : MNG_LIST) {
          String mgnNt = JsonPath.read(document, "$.response.docs[0]." + mng);
          if (StringUtils.isNotBlank(mgnNt) && (!nt.equals(mgnNt.trim()))) {
            return mgnNt;
          }
        }
      }
    } catch (DateTimeParseException e) {
      LOGGER.error("Date Parse Error", e);
    } catch (PathNotFoundException e) {
      LOGGER.error("Json Parse Error", e);
    }
    return null;
  }

  private String getUsername(Object document) {
    try {
      String preferredName = JsonPath.read(document, "$.response.docs[0].preferred_name");
      String lastName = JsonPath.read(document, "$.response.docs[0].last_name");
      return String.format("%s, %s", preferredName, lastName);
    } catch (PathNotFoundException e) {
      LOGGER.error("Json Parse Error", e);
    }
    return null;
  }

  public void generateScheduleJobInstance() {
    LOGGER.info("Start generate scheduler job instance [{}]", System.currentTimeMillis());
    SimpleDateFormat formatter = new SimpleDateFormat(DateUtil.TS0);
    String now = formatter.format(new Date());
    List<ScheduleJob> scheduleJobs = scheduleJobRepository.findExecutableJob(now);
    for (ScheduleJob scheduleJob : scheduleJobs) {
      Optional.of(init(scheduleJob)).ifPresent(i -> teardown(scheduleJob));
    }
  }

  private ScheduleHistory init(ScheduleJob scheduleJob) {
    try {
      LOGGER.info("Job instance start to init - {}", JsonUtil.toJson(scheduleJob));
      ScheduleHistory scheduleHistory = new ScheduleHistory();
      scheduleHistory.setScheduleJob(scheduleJob);
      scheduleHistory.setStartTime(new Date());
      scheduleHistory.setJobRunStatus(PENDING);
      scheduleHistoryRepository.save(scheduleHistory);
      LOGGER.info("Generate Job Instance {}", JsonUtil.toJson(scheduleHistory));
      return scheduleHistory;
    } catch (Exception e) {
      LOGGER.error("Job init failed!", e);
    }
    return null;
  }

  private void teardown(ScheduleJob scheduleJob) {
    try {
      LOGGER.info("Job start to destroy - {}", scheduleJob.getJobName());
      Date lastRunTime = scheduleJob.getNextRunTime();
      LOGGER.info("Get scheduler job [{}] last run time: {}",
          scheduleJob.getId(), lastRunTime);
      scheduleJob.setLastRunTime(lastRunTime);
      ScheduleTime scheduleTime = JsonUtil.fromJson(scheduleJob.getScheduleTime(),
          ScheduleTime.class);
      if ("ONETIME".equals(scheduleTime.getJobType())) {
        // for one time job
        LOGGER.info("Inactive ONETIME Schedule job [{}]", scheduleJob.getId());
        scheduleJob.setNextRunTime(null);
        scheduleJob.setStatus(0);
      } else {
        Date nextRunTime = DateUtil.getCronDate(scheduleJob.getCronExpression(),
            lastRunTime);
        LOGGER.info("Set scheduler job [{}] next run time as {}"
            , scheduleJob.getId(), nextRunTime);
        scheduleJob.setNextRunTime(nextRunTime);
      }
      LOGGER.info("Job is destroyed - {}", JsonUtil.toJson(scheduleJob));
      scheduleJobRepository.save(scheduleJob);
    } catch (Exception e) {
      LOGGER.info("Job destroy failed!", e);
    }
  }

  public void executeScheduleJob() {
    LOGGER.info("Start execute scheduler job [{}]", System.currentTimeMillis());
    List<ScheduleHistory> scheduleHistories = scheduleHistoryRepository.getRunnableScheduleHistory();

    Map<Long, List<ScheduleHistory>> scheduleJobGroup =
        scheduleHistories.stream().collect(Collectors
            .groupingBy(history -> history.getScheduleJob().getId()));

    for (List<ScheduleHistory> scheduleHistoryList : scheduleJobGroup.values()) {
      ScheduleJob scheduleJob = scheduleHistoryList.get(0).getScheduleJob();
      switch (ScheduleJobType.valueOf(scheduleJob.getType())) {
        case Notebook:
          notebookScheduler.handleJobInstanceList(scheduleHistoryList);
          break;
        case DataMove:
          dataMoveScheduler.handleJobInstanceList(scheduleHistoryList);
          break;
        case DataValidate:
          dataValidateScheduler.handleJobInstanceList(scheduleHistoryList);
          break;
        case MetaTable:
          metaTableScheduler.handleJobInstanceList(scheduleHistoryList);
          break;
      }
    }
  }

  public void cleanInactivateScheduleJob() {
    LOGGER.info("Start inactivate scheduler job [{}]", System.currentTimeMillis());
    List<Map<String, Object>> inactiveJobLists = scheduleJobRepository.getInactiveSchedulerJobs();
    for (Map<String, Object> job : inactiveJobLists) {
      LOGGER.info("Clean inactive job: {}", job);
      Long jobId = (Long) job.get("id");
      String jobName = (String) job.get("job_name");
      BigInteger num = (BigInteger) job.get("num");
      String nt = (String) job.get("nt");
      if (scheduleJobRepository.updateScheduleJobStatus(0, jobId) > 0) {
        mailService.sendScheduleInactivateEmail(jobId, jobName, num, nt + "@ebay.com");
      }
    }
  }

}
