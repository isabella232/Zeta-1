package com.ebay.dss.zds.service.datamove;

import com.ebay.dss.zds.common.JsonUtil;
import com.ebay.dss.zds.model.DataMoveDetail;
import com.google.common.collect.Maps;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

@Component
public class VDMVIEW2HDJob extends DataMoveJob {
  private static final Logger LOGGER = LoggerFactory.getLogger(VDMVIEW2HDJob.class);

  @Autowired
  private WSMService wsmService;

  @Override
  void initialize(DataMoveDetail dataMoveDetail) {
  }


  public void submitWSMViewJob(DataMoveDetail dataMoveDetail) {
    if (dataMoveDetail.getHistory().getStatus() == 0 && dataMoveDetail.getStep() == 0) {

      ResponseEntity<String> responseEntity = wsmService.submitViewMoveJob(dataMoveDetail);
      String responseBody = responseEntity.getBody();
      LOGGER.info("WSM Submit VDM View Move Job API response statue: {}"
          , responseEntity.getStatusCodeValue());
      LOGGER.info("WSM Submit VDM View API response: {}", responseBody);
      Configuration conf = Configuration.defaultConfiguration()
          .addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL)
          .addOptions(Option.SUPPRESS_EXCEPTIONS);
      if (responseEntity.getStatusCodeValue() == 200) {
        String viewStatus = JsonPath.using(conf).parse(responseBody).read("$.status");
        String viewDb = JsonPath.using(conf).parse(responseBody).read("$.targetViewDb");
        LOGGER.info("Get View Database: {}", viewDb);
        dataMoveDetail.setViewDb(viewDb);
        switch (viewStatus) {
          case "SUCCESS":
            dataMoveDetail.getHistory().setStatus(1);
            break;
          case "FAIL":
          case "PARTIAL SUCCESS":
            List<Map<String, Object>> viewResults = JsonPath
                .using(conf)
                .parse(responseBody)
                .read("$.views", List.class);
            Map<String, Object> failJobs = parseMultiViewJobStatus(viewResults);
            dataMoveDetail.getHistory().setStatus(2);
            dataMoveDetail.getHistory().setLog(JsonUtil.toJson(failJobs));
            break;
          default:
            // Unknown error, set status as fail
            dataMoveDetail.getHistory().setLog(responseBody);
            dataMoveDetail.getHistory().setStatus(2);
            break;
        }
      }
    }
  }

  @Override
  void startMove(DataMoveDetail dataMoveDetail) {
    submitWSMViewJob(dataMoveDetail);
  }

  private Map<String, Object> parseMultiViewJobStatus(List<Map<String, Object>> viewResults) {
    Map<String, Object> failJobs = Maps.newLinkedHashMap();
    if (CollectionUtils.isEmpty(viewResults)) {
      return failJobs;
    }
    for (Map<String, Object> job : viewResults) {
      String status = (String) job.getOrDefault("status", "");
      switch (status) {
        case "SUCCESS":
          break;
        case "FAIL":
          failJobs.put((String) job.get("view"), job.get("comment"));
          break;
      }
    }
    return failJobs;
  }
}
