package com.ebay.dss.zds.service;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import com.ebay.dss.zds.model.History;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.ebay.dss.zds.common.JsonUtil;
import com.ebay.dss.zds.model.ZetaResponse;
import com.ebay.dss.zds.dao.DataValidateRepository;
import com.ebay.dss.zds.dao.HistoryRepository;
import com.ebay.dss.zds.exception.ToolSetCheckException;
import com.ebay.dss.zds.model.DataValidateDetail;

/**
 * Created by zhouhuang on 2018年5月17日
 */
@Service
public class DataValidationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(DataValidationService.class);

  @Autowired
  private DataValidateRepository dataValidateRepository;

  @Autowired
  private HistoryRepository historyRepository;

  @Resource(name = "resttemplate")
  private RestTemplate restTemplate;

  private static String COMPARE_TABLE;


  @Value("${ido.host.url}")
  private void initServiceUrl(String hostUrl) {
    COMPARE_TABLE = hostUrl +
        "/api/recon/compareTable?email=%s@ebay.com&platform1=%s&platform1_tablename=%s&platform2=%s&platform2_tablename=%s&checktype=checksum";
  }

  public ZetaResponse<Object> save(DataValidateDetail dataValidateDetail) {
    dataValidateDetail.getHistory().setType(2);
    dataValidateDetail.getHistory().setCreateDate(new Date());
    dataValidateDetail.setCreateDate(new Date());
    String url = String.format(COMPARE_TABLE, dataValidateDetail.getHistory().getNt().trim(),
        dataValidateDetail.getHistory().getSourcePlatform().trim(),
        dataValidateDetail.getHistory().getSourceTable().trim().replace("default.", ""),
        dataValidateDetail.getHistory().getTargetPlatform().trim(),
        dataValidateDetail.getHistory().getTargetTable().trim()).replace("default.", "");
    if (dataValidateDetail.getSourceBatchAccount() != null
        && dataValidateDetail.getSourceBatchAccount().trim().length() > 0) {
      url = url + "&platform1_account=" + dataValidateDetail.getSourceBatchAccount().trim();
    }
    if (dataValidateDetail.getTargetBatchAccount() != null
        && dataValidateDetail.getTargetBatchAccount().trim().length() > 0) {
      url = url + "&platform2_account=" + dataValidateDetail.getTargetBatchAccount().trim();
    }
    if (dataValidateDetail.getSourceFilter() != null && dataValidateDetail.getSourceFilter().trim().length() > 0) {
      url = url + "&platform1_condition=" + dataValidateDetail.getSourceFilter().trim().replace("\n", " ");
    }
    if (dataValidateDetail.getTargetFilter() != null && dataValidateDetail.getTargetFilter().trim().length() > 0) {
      url = url + "&platform2_condition=" + dataValidateDetail.getTargetFilter().trim().replace("\n", " ");
    }

    LOGGER.info("Call Data Validation Service {}", url);
    Map<String, Object> dqRes = restTemplate.getForObject(url, Map.class);
    if (dqRes.containsKey("err")) {
      throw new ToolSetCheckException((String) dqRes.get("err"));
    } else if (dqRes.containsKey("data")) {
      Map<String, Object> data = (Map<String, Object>) dqRes.get("data");
      dataValidateDetail.setTaskId((int) data.get("id"));
    } else {
      throw new ToolSetCheckException(JsonUtil.toJson(dqRes));
    }
    dataValidateRepository.save(dataValidateDetail);
    return new ZetaResponse<>("Success", HttpStatus.OK);
  }

  public List<Map<String, Object>> getHistory(String nt) {
    return historyRepository.getHistoryList(nt, 2);
  }

  public History getHistoryDetail(Long historyId) {
    return historyRepository.getHistoryByHistoryId(historyId);
  }

}
