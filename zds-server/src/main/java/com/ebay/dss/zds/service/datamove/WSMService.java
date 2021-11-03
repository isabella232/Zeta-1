package com.ebay.dss.zds.service.datamove;

import com.ebay.dss.zds.common.JsonUtil;
import com.ebay.dss.zds.common.PropertiesUtil;
import com.ebay.dss.zds.exception.ToolSetCheckException;
import com.ebay.dss.zds.model.DataMoveDetail;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.springframework.http.HttpMethod.PUT;

@Service
public class WSMService {

  private static final Logger LOGGER = LoggerFactory.getLogger(WSMService.class);

  @Autowired
  @Qualifier("wsm-template")
  private RestTemplate rest;

  @Autowired
  TableMetaService tableMetaService;

  @Value("${spring.profiles.active}")
  private String env;


  protected String getServiceUrl(WSMServiceType wsmServiceType) {
    String hostUrl = PropertiesUtil.getDatamoveProperties("host.url").trim();
    LOGGER.info("Get WSM host url: {}", hostUrl);
    return hostUrl + PropertiesUtil.getDatamoveProperties(wsmServiceType.getValue()).trim();
  }

  enum WSMServiceType {
    GET_DB("getdatabase.url"),
    GET_TABLE("gettable.url"),
    GET_WORKSPACE("getworkspace.url"),
    SUBMIT_JOB("submitjob.url"),
    GET_JOBSTATUS("getjob.url"),
    RETRY_JOB("retryjob.url"),
    VALIDATE_TABLE("validatetable.url"),
    CANCEL_JOB("canceljob.url"),
    GET_VIEW("getview.url"),
    SUBMIT_VIEW_JOB("submitviewjob.url");
    String value;

    WSMServiceType(String value) {
      this.value = value;
    }

    String getValue() {
      return value;
    }
  }

  private String convertSystemName(String systemName) {
    return systemName.trim().equalsIgnoreCase("numozart") ? "mozart" : systemName;
  }

  public List<String> getDatabases(String systemName, String userName, boolean isRealtime) {
    String url = getServiceUrl(WSMServiceType.GET_DB);
    systemName = convertSystemName(systemName);
    LOGGER.info("Get Database parameter: {},{}", systemName, userName);
    List res = getVDMDatabase(systemName, userName, url, isRealtime);
    if (!isRealtime && res.isEmpty()) {
      res = getVDMDatabase(systemName, userName, url, true);
    }
    return res;
  }

  private List<String> getVDMDatabase(String systemName, String userName,
                                      String url, boolean isRealtime) {
    LOGGER.info("Get Database without cache:[{}],[{}]", systemName, userName);
    return JsonUtil.fromJson(callWsmApi(HttpMethod.GET, WSMServiceType.GET_DB.name(),
        String.format(url, systemName.trim(), userName, isRealtime, isRealtime), true)
        .getBody(), new TypeReference<List<String>>() {
    });
  }

  public List<String> getTablesInDB(String systemName, String databaseName, boolean isRealtime) {
    String url = getServiceUrl(WSMServiceType.GET_TABLE);
    systemName = convertSystemName(systemName);
    LOGGER.info("Get Table parameter: {},{}", systemName, databaseName);
    List res = getVDMTables(systemName, databaseName, url, isRealtime);
    if (!isRealtime && res.isEmpty()) {
      res = getVDMTables(systemName, databaseName, url, true);
    }
    return res;
  }

  private List<String> getVDMTables(String systemName, String databaseName
      , String url, boolean isRealtime) {
    LOGGER.info("Get Database without cache:[{}],[{}]", systemName, databaseName);
    return JsonUtil.fromJson(callWsmApi(HttpMethod.GET, WSMServiceType.GET_TABLE.name(),
        String.format(url, systemName.trim(), databaseName.trim(), isRealtime, isRealtime), true)
        .getBody(), new TypeReference<List<String>>() {
    });
  }

  @Deprecated
  public List<String> getWorkspaces(String systemName, String userName) {
    String url = getServiceUrl(WSMServiceType.GET_WORKSPACE);
    LOGGER.info("Get Workspace parameter: {},{}", systemName, userName);
    ResponseEntity<String> responseEntity = callWsmApi(HttpMethod.GET,
        WSMServiceType.GET_WORKSPACE.name(),
        String.format(url, userName, true, true), false);
    switch (responseEntity.getStatusCodeValue()) {
      case 200:
        return parsePlatformWorkspace(responseEntity.getBody().toLowerCase()
            , systemName.toLowerCase());
      case 404:
        return Lists.newArrayList();
      default:
        throw new ToolSetCheckException(
            "GET_WORKSPACE service occurs error, please have a retry later!");
    }
  }

  private List<String> parsePlatformWorkspace(String content, String platform) {
    String path = String.format("$.[?(@.datasystem.name == \"%s\")].name", platform);
    Configuration conf = Configuration.defaultConfiguration()
        .addOptions(Option.ALWAYS_RETURN_LIST)
        .addOptions(Option.SUPPRESS_EXCEPTIONS);
    LOGGER.info("JsonPath -- {}", path);
    return JsonPath.using(conf).parse(content).read(path);
  }

  public ResponseEntity<String> submitMoveJob(DataMoveDetail dataMoveDetail) {
    String url = getServiceUrl(WSMServiceType.SUBMIT_JOB);
    return callWsmApi(HttpMethod.POST, WSMServiceType.SUBMIT_JOB.name(), url
        , false, constructVDMJobMap(dataMoveDetail));
  }

  private Map<String, Object> constructVDMJobMap(DataMoveDetail dataMoveDetail) {
    List<String> sourceTables = JsonUtil.fromJson(dataMoveDetail.getHistory().getSourceTable(),
        new TypeReference<List<String>>() {
        });
    Map<String, Object> params = Maps.newLinkedHashMap();
    params.put("user", dataMoveDetail.getHistory().getNt());
    params.put("sourceSystem", dataMoveDetail.getHistory().getVDMSourcePlatform());
    params.put("sourceDatabase", getVDMSourceDB(sourceTables));
    params.put("sourceTables", getVDMSourceTbls(sourceTables));
    params.put("targetSystem", dataMoveDetail.getHistory().getTargetPlatform());
    params.put("targetDatabase", dataMoveDetail.getHistory().getTargetTable());
    params.put("format", "parquet");
    params.put("partition", Maps.newHashMap());
    params.put("writeType", dataMoveDetail.getIsDrop() == 1 ? "Overwrite" : "error");
    LOGGER.info("Submit Job parameter: {}", JsonUtil.toJson(params));
    return params;
  }

  private String getVDMSourceDB(List<String> sourceTables) {
    return tableMetaService.getDBName(sourceTables.get(0));
  }

  public String getVDMSourceDB(DataMoveDetail dataMoveDetail) {
    List<String> sourceTables = JsonUtil.fromJson(dataMoveDetail.getHistory().getSourceTable(),
        new TypeReference<List<String>>() {
        });
    return getVDMSourceDB(sourceTables);
  }

  private List<String> getVDMSourceTbls(List<String> sourceTables) {
    return sourceTables.stream().map(tbl -> tableMetaService.getTableName(tbl))
        .filter(StringUtils::isNotBlank)
        .map(String::trim)
        .collect(Collectors.toList());
  }

  public String getMoveJobStatus(long jobId) {
    String url = getServiceUrl(WSMServiceType.GET_JOBSTATUS);
    LOGGER.info("Get Job Status: {}", jobId);
    ResponseEntity<String> responseEntity = callWsmApi(HttpMethod.GET,
        WSMServiceType.GET_JOBSTATUS.name(),
        String.format(url, jobId), false);
    if (responseEntity.getStatusCode() == HttpStatus.OK) {
      return responseEntity.getBody();
    }
    return null;
  }

  public int retryVDMJob(long jobId) {
    String url = getServiceUrl(WSMServiceType.RETRY_JOB);
    LOGGER.info("Retry Job: {}", jobId);
    ResponseEntity<String> responseEntity = callWsmApi(PUT,
        WSMServiceType.RETRY_JOB.name(),
        String.format(url, jobId), true);
    Map res = JsonUtil.fromJson(responseEntity.getBody(), Map.class);
    return (int) res.getOrDefault("id", 0);

  }

  public Object validateVDMTable(DataMoveDetail dataMoveDetail) {
    String url = getServiceUrl(WSMServiceType.VALIDATE_TABLE);
    ResponseEntity responseEntity = callWsmApi(HttpMethod.POST
        , WSMServiceType.VALIDATE_TABLE.name(), url
        , false, constructVDMJobMap(dataMoveDetail));
    switch (responseEntity.getStatusCodeValue()) {
      case 200:
      case 409:
        return responseEntity.getBody();
      default:
        Map res = JsonUtil.fromJson(responseEntity.getBody().toString(), Map.class);
        throw new ToolSetCheckException(
            res.getOrDefault("message", "Validate Fail").toString());
    }

  }

  public List<String> getViewsInDB(String userName, String systemName, String databaseName) {
    String url = getServiceUrl(WSMServiceType.GET_VIEW);
    systemName = convertSystemName(systemName);
    LOGGER.info("Get Table parameter: {},{}", systemName, databaseName);
    LOGGER.info("Get Database without cache:[{}],[{}]", systemName, databaseName);
    Map response = JsonUtil.fromJson(callWsmApi(HttpMethod.GET, WSMServiceType.GET_VIEW.name(),
        String.format(url, userName, systemName.trim(), databaseName.trim()), true)
        .getBody(), new TypeReference<Map<String, Object>>() {
    });
    return response.containsKey("views") ? (List<String>) response.get("views") : Lists.newArrayList();
  }

  public ResponseEntity<String> submitViewMoveJob(DataMoveDetail dataMoveDetail) {
    List<String> sourceViews = JsonUtil.fromJson(dataMoveDetail.getHistory().getSourceTable(),
        new TypeReference<List<String>>() {
        });
    String url = String.format(getServiceUrl(WSMServiceType.SUBMIT_VIEW_JOB),
        dataMoveDetail.getHistory().getNt(),
        dataMoveDetail.getHistory().getVDMSourcePlatform(),
        getVDMSourceDB(sourceViews),
        dataMoveDetail.getHistory().getTargetPlatform(),
        dataMoveDetail.getHistory().getTargetTable()
    );
    Map<String, Object> views = Maps.newLinkedHashMap();
    views.put("views", getVDMSourceTbls(sourceViews));
    return callWsmApi(HttpMethod.POST, WSMServiceType.SUBMIT_VIEW_JOB.name(), url, true, views);
  }

  private ResponseEntity<String> callWsmApi(HttpMethod httpMethod, String module,
                                            String url, boolean isCheck,
                                            Map<String, Object>... params) {
    ResponseEntity<String> responseEntity = null;
    LOGGER.info("{} [{}] url: {}", httpMethod.name(), module, url);
    switch (httpMethod) {
      case POST:
        // create headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(params[0], headers);
        LOGGER.info("POST Body: {}", entity);
        responseEntity = rest.postForEntity(url, entity, String.class);
        break;
      case GET:
        responseEntity = rest.getForEntity(url, String.class);
        break;
      case PUT:
        responseEntity = rest.exchange(url, PUT, null, String.class);
    }
    LOGGER.info("{} [{}] response: {}", httpMethod.name(), module, responseEntity);
    if (isCheck && Objects.nonNull(responseEntity) &&
        responseEntity.getStatusCode() != HttpStatus.OK) {
      throw new ToolSetCheckException(parseErrorResponse(responseEntity.getBody()));
    }
    return responseEntity;
  }

  private String parseErrorResponse(String response) {
    try {
      return JsonUtil.fromJson(response, Map.class)
          .getOrDefault("message", response).toString();
    } catch (Exception e) {
      return response;
    }
  }
}