package com.ebay.dss.zds.service.datamove;

import com.ebay.dss.zds.common.JsonUtil;
import com.ebay.dss.zds.model.DataMoveDetail;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Maps;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class VDM2HDJob extends DataMoveJob {
  private static final Logger LOGGER = LoggerFactory.getLogger(VDM2HDJob.class);

  @Autowired
  private WSMService wsmService;

  @Override
  void initialize(DataMoveDetail dataMoveDetail) {
    submitWSMJob(dataMoveDetail);
  }


  public void submitWSMJob(DataMoveDetail dataMoveDetail) {
    if (dataMoveDetail.getHistory().getStatus() == 0 && dataMoveDetail.getStep() == 0) {

      ResponseEntity<String> responseEntity = wsmService.submitMoveJob(dataMoveDetail);
      String responseBody = responseEntity.getBody();
      LOGGER.info("WSM Submit VDM Move Job API response statue: {}"
          , responseEntity.getStatusCodeValue());
      LOGGER.info("WSM Submit VDM Move Job API response: {}", responseBody);
      Configuration conf = Configuration.defaultConfiguration()
          .addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL)
          .addOptions(Option.SUPPRESS_EXCEPTIONS);
      switch (responseEntity.getStatusCodeValue()) {
        case 200:
          //submit success, get task id
          Integer taskId = JsonPath.using(conf).parse(responseBody).read("$.id");
          if (Objects.isNull(taskId)) {
            return;
          }
          dataMoveDetail.setTaskId(taskId);
          dataMoveDetail.setStep(1);
          break;
        case 406:
          //unacceptable job, set status as fail
          dataMoveDetail.getHistory().setLog(
              JsonPath.using(conf).parse(responseBody).read("$.message"));
          dataMoveDetail.getHistory().setStatus(2);
          break;
        default:
          //unknow error, set status as fail and will go retry logic
          String errorLog = JsonPath.using(conf).parse(responseBody).read("$.message");
          dataMoveDetail.setErrorLog(Objects.isNull(errorLog) ? responseBody : errorLog);
          dataMoveDetail.getHistory().setStatus(2);
          break;
      }
    }
  }

  @Override
  void startMove(DataMoveDetail dataMoveDetail) {
    if (dataMoveDetail.getHistory().getStatus() == 0 && dataMoveDetail.getStep() == 1) {

      String jobResponse = wsmService.getMoveJobStatus(dataMoveDetail.getTaskId());
      LOGGER.info("WSM Get job API response: {}", jobResponse);
      if (Objects.nonNull(jobResponse)) {
        parseMultiJobStatus(jobResponse, dataMoveDetail);
      }
    }
  }

  @Deprecated
  private void parseJobStatus(String jobResponse, DataMoveDetail dataMoveDetail) {
    Configuration conf = Configuration.defaultConfiguration()
        .addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL)
        .addOptions(Option.SUPPRESS_EXCEPTIONS);
    String status = JsonPath.using(conf).parse(jobResponse).read("$.status");
    switch (status) {
      case "SUCCESS":
        dataMoveDetail.getHistory().setStatus(1);
        break;
      case "FAILED":
        dataMoveDetail.getHistory().setStatus(2);
        dataMoveDetail.getHistory().setLog(
            JsonPath.using(conf).parse(jobResponse).read("$.comment"));
        break;
    }
  }

  private void parseMultiJobStatus(String jobResponse, DataMoveDetail dataMoveDetail) {
    List<Map<String, Object>> jobs = JsonUtil.fromJson(jobResponse,
        new TypeReference<List<Map<String, Object>>>() {
        });
    if (!jobs.isEmpty() && !checkParentJobFinished(jobs.get(0))) {
      return;
    }
    Map<String, Object> failJobs = Maps.newLinkedHashMap();
    int sourceTableNum = JsonUtil.fromJson(dataMoveDetail.getHistory().getSourceTable()
        , List.class).size();
    int finishJobNum = 0;
    for (Map<String, Object> job : jobs) {
      String status = (String) job.getOrDefault("status", "");
      switch (status) {
        case "SUCCESS":
          finishJobNum += 1;
          break;
        case "CANCELLED":
        case "FAILED":
          finishJobNum += 1;
          String tableName = String.format("%s.%s", job.get("sourceDatabase"), job.get("sourceTable"));
          failJobs.put(tableName, job.get("comment"));
          break;
      }
    }
    LOGGER.info("Job Done Num: {};Source Table Num: {}", finishJobNum, sourceTableNum);
    if (finishJobNum < sourceTableNum) {
      // not all jobs done
      return;
    }
    if (failJobs.isEmpty()) {
      dataMoveDetail.getHistory().setStatus(1);
    } else {
      dataMoveDetail.getHistory().setStatus(2);
      dataMoveDetail.getHistory().setLog(JsonUtil.toJson(failJobs));
    }
  }


  private boolean checkParentJobFinished(Map<String, Object> job) {
    Map<String, Object> parentJob = (Map<String, Object>) job.get("vdmMoveJob");
    String status = (String) parentJob.getOrDefault("status", "");
    switch (status) {
      case "RUNNING":
      case "RETRY":
        return false;
      default:
        return true;
    }
  }
}
