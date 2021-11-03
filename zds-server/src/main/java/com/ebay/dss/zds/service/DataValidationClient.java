package com.ebay.dss.zds.service;

import com.ebay.dss.zds.exception.ToolSetCheckException;
import com.ebay.dss.zds.model.*;
import com.ebay.dss.zds.service.schedule.DataValidateScheduler;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.ebay.dss.zds.common.JsonUtil;
import com.ebay.dss.zds.dao.DataValidateRepository;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Resource;

import static com.ebay.dss.zds.service.schedule.ScheduleJobType.DataValidate;

@Component
public class DataValidationClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(DataValidationClient.class);

  @Autowired
  private DataValidateRepository dataValidateRepository;

  @Autowired
  private DataValidateScheduler dataValidateScheduler;

  @Resource(name = "error-handle-rest-template")
  private RestTemplate restTemplate;

  @Autowired
  MailService mailService;

  private static String GET_STATUS;
  private static String GET_DETAILS;
  private static String GET_QUERY;

  @Value("${ido.host.url}")
  private void initServiceUrl(String hostUrl) {
    GET_STATUS = hostUrl + "/api/recon/getStatusADPO?id=%s";
    GET_DETAILS = hostUrl + "/api/recon/getDetailADPO?id=%s";
    GET_QUERY = hostUrl + "/api/external/recon/rulesapi/getOnetimeReconRuleSql?ruleId=%s";
  }

  public void executeDatavaliate() {
    LOGGER.info("Begin to check data validate task status [{}]", System.currentTimeMillis());
    List<DataValidateDetail> dataValidateDetails = dataValidateRepository.findByHistory_Status(0);
    dataValidateDetails.forEach(dataValidateDetail -> validate(dataValidateDetail));
  }

  public void validate(DataValidateDetail dataValidateDetail) {
    if (dataValidateDetail.getHistory().getStartTime() == null) {
      dataValidateDetail.getHistory().setStartTime(new Date());
      dataValidateRepository.save(dataValidateDetail);
    }
    JobResult jobResult = validate(dataValidateDetail.getTaskId());
    if (jobResult != null) {
      int status = jobResult.getStatus() ? 1 : 2;
      Date endTime = new Date();
      dataValidateDetail.setResult(jobResult.getDetails());
      dataValidateDetail.getHistory().setLog(jobResult.getOutput());
      dataValidateDetail.getHistory().setStatus(status);
      dataValidateDetail.getHistory().setEndTime(endTime);
      dataValidateDetail.setQuery(getDataValidateQuery(dataValidateDetail.getTaskId()));
      dataValidateRepository.save(dataValidateDetail);
      boolean isScheduleJob = Optional.ofNullable(dataValidateScheduler.updateScheduleHistoryStatus(
          dataValidateDetail.getHistory().getHistoryId(),
          jobResult.getStatus(),
          jobResult.getOutput(), DataValidate)).isPresent();
      if (!isScheduleJob) {
        // send email
        mailService.sendDataValidateDoneEmail(dataValidateDetail);
      }
    }
  }

  public JobResult validate(int taskId) {
    LOGGER.info("Begin to check task status [{}]", taskId);
    JobResult jobResult = new JobResult();
    try {
      int flag = getJobStatus(taskId, jobResult);
      if (flag == 1) {
        return null;
      }
      LOGGER.info("{} data validation is done. JobResult:{}", taskId, jobResult);
    } catch (Exception e) {
      LOGGER.error("Data validation failed!", e);
      jobResult.setStatus(false);
      jobResult.setOutput(e.getMessage());
    }

    return jobResult;

  }

  private int getJobStatus(int taskId, JobResult jobResult) {
    int flag = isValidateTaskComplete(taskId, jobResult);

    if (flag == 1) {
      LOGGER.info("Task [{}] is not finished yet...", String.format(GET_STATUS, taskId));
    } else if (flag == 0 || flag == -2) {
      // get data validation result
      flag = getJobResult(taskId, jobResult) ? flag : 1;

    }
    return flag;
  }

  private boolean getJobResult(int taskId, JobResult jobResult) {
    ResponseEntity<String> responseEntity = restTemplate.getForEntity(String.format(GET_DETAILS, taskId), String.class);
    if (responseEntity.getStatusCode() == HttpStatus.OK) {
      Configuration conf = Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS);
      List valueList = JsonPath.using(conf).parse(responseEntity.getBody()).read("$.data.value");
      jobResult.setDetails(JsonUtil.toJson(valueList));
      return true;
    }
    return false;
  }

  private int isValidateTaskComplete(int taskId, JobResult jobResult) {
    ResponseEntity<String> responseEntity = restTemplate.getForEntity(String.format(GET_STATUS, taskId), String.class);
    if (responseEntity.getStatusCode() == HttpStatus.OK && (!responseEntity.getBody().contains("no result"))) {
      Configuration conf = Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS);
      String status = JsonPath.using(conf).parse(responseEntity.getBody()).read("$.data.value[0].status");
      String failReason = JsonPath.using(conf).parse(responseEntity.getBody()).read("$.data.value[0].fail_reason");
      if ("pass".equalsIgnoreCase(status)) {
        LOGGER.debug("Task [{}] finished successfully!", String.format(GET_STATUS, taskId));
        jobResult.setStatus(true);
        return 0;
      } else if ("running".equalsIgnoreCase(status) && Objects.nonNull(failReason)) {
        LOGGER.debug("Task [{}] finished failed!", String.format(GET_STATUS, taskId));
        jobResult.setStatus(false);
        jobResult.setOutput(failReason);
        return -1;
      } else if ("fail".equalsIgnoreCase(status)) {
        LOGGER.debug("Task [{}] finished failed!", String.format(GET_STATUS, taskId));
        jobResult.setStatus(false);
        jobResult.setOutput("Alert! Data is NOT consistent");
        return -2;
      } else if ("down".equalsIgnoreCase(status)) {
        throw new ToolSetCheckException("Job Failed with Unknown issue. Please have a retry.");
      }
    }
    // not finish
    return 1;

  }

  private String getDataValidateQuery(int taskId) {
    try {
      for (int i = 0; i < 6; i++) {
        ResponseEntity<String> responseEntity = restTemplate.getForEntity(String.format(GET_QUERY, taskId), String.class);
        if (responseEntity.getStatusCode() == HttpStatus.OK && JsonUtil.fromJson(responseEntity.getBody(), List.class).size() > 1) {
          return responseEntity.getBody();
        }
        try {
          Thread.sleep(15000);
        } catch (InterruptedException e) {

        }
      }
    } catch (Exception e) {
      LOGGER.error("Get Data validation query failed", e);
    }
    return null;
  }

}