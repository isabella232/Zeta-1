package com.ebay.dss.zds.service.datamove;

import com.ebay.dss.zds.common.DateUtil;
import com.ebay.dss.zds.common.JsonUtil;
import com.ebay.dss.zds.common.PropertiesUtil;
import com.ebay.dss.zds.exception.ToolSetCheckException;
import com.ebay.dss.zds.model.DataMoveDetail;
import com.ebay.dss.zds.model.JobResult;
import com.ebay.dss.zds.model.Platform;
import com.ebay.dss.zds.model.Table;
import com.ebay.dss.zds.service.ZetaUserService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Splitter;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static com.ebay.dss.zds.service.datamove.TableMetaService.MetadataType.REALTIME;

@Component
public class TD2HDJob extends DataMoveJob {

  private static final Logger LOGGER = LoggerFactory.getLogger(TD2HDJob.class);

  @Resource(name = "error-handle-rest-template")
  protected RestTemplate restTemplate;

  @Autowired
  private ZetaUserService zetaUserService;

  @Autowired
  private TableMetaService tableMetaService;

  @Autowired
  private DataMoveTempViewHandler dataMoveTempViewHandler;

  // 1.mozart 2.hercules 3.dbName 4.tableName 5.tdId 6.tdPwd
  // 7.hdBatchId 8.outputLoc 9.sql
  private static final String TD_BRIDGE_MOVE_SHELL = "%s %s %s %s %s %s %s %s %s \\\"%s\\\"";
  private static final String TOUCH_FILE_GENERATOR = "touch %s";
  //  private static final String DAPPER_STATUS_CHECK = "https://dapper.corp.ebay.com/MixtaskAPI/MixTaskDetail?taskId=%s&username=%s";
  //  private static final String DAPPER_CREATE_TASK = "https://dapper.corp.ebay.com/MixtaskAPI/commitMixtask";
  private static String DAPPER_STATUS_CHECK;
  private static String DAPPER_CREATE_TASK;
  private static String DAPPER_TASK_SHOW;
  private static final String ETL_SERVER = "etl.server";
  private static final String ETL_SCRIPT = "etl.script";
  private static final String EXTRA_CMD = "extra.command";


  @Value("${dapper.host.url}")
  private void initServiceUrl(String hostUrl) {
    DAPPER_STATUS_CHECK = hostUrl + "/MixtaskAPI/MixTaskDetail?taskId=%s&username=%s";
    DAPPER_CREATE_TASK = hostUrl + "/MixtaskAPI/commitMixtask";
    DAPPER_TASK_SHOW = hostUrl + "/mixtasks/show?id=%s";
  }

  @Override
  void initialize(DataMoveDetail dataMoveDetail) {
    generateTD2HDQuery(dataMoveDetail);
  }


  protected void generateTD2HDQuery(DataMoveDetail dataMoveDetail) {
    if (dataMoveDetail.getHistory().getStatus() == 0 && dataMoveDetail.getStep() == 0) {

      List<String> tableInfo = Splitter.on(".").trimResults().omitEmptyStrings()
          .splitToList(dataMoveDetail.getHistory().getSourceTable());
      Table table = tableMetaService.getTableSchema(dataMoveDetail.getHistory().getSourcePlatform().trim(), tableInfo.get(0),
          tableInfo.get(1), REALTIME);
      //TD Bridge Query
      dataMoveDetail.setQuery(tableMetaService.generateTDBridgeSQL(table, dataMoveDetail.getFilter()));
      dataMoveDetail.setTdBridgeAvro(tableMetaService.getTDBridgeAvroUploadPath(dataMoveDetail));
      dataMoveTempViewHandler.handle(dataMoveDetail);
      LOGGER.info("{} Generate TD to HD Move Queries [{}]", dataMoveDetail, getSparkSQL(dataMoveDetail));
      dataMoveDetail.setStep(1);
    }

  }

  @Override
  void startMove(DataMoveDetail dataMoveDetail) {
    LOGGER.info("TD2HDJob {} Begin to Move", dataMoveDetail);

    UnaryOperator<DataMoveDetail> stepOneProcessing = (DataMoveDetail dmd) -> runStepOne(dmd);
    UnaryOperator<DataMoveDetail> stepTwoProcessing = (DataMoveDetail dmd) -> runStepTwo(dmd);
    UnaryOperator<DataMoveDetail> stepThreeProcessing = (DataMoveDetail dmd) -> runStepThree(dmd);
    UnaryOperator<DataMoveDetail> stepFourProcessing = (DataMoveDetail dmd) -> runStepFour(dmd);
    Function<DataMoveDetail, DataMoveDetail> pipeline = stepOneProcessing.andThen(stepTwoProcessing)
        .andThen(stepThreeProcessing).andThen(stepFourProcessing);
    pipeline.apply(dataMoveDetail);

  }

  public DataMoveDetail runStepTwo(DataMoveDetail dataMoveDetail) {
    if (dataMoveDetail.getHistory().getStatus() == 0 && dataMoveDetail.getStep() == 2) {
      LOGGER.info("{} Move Step 2", dataMoveDetail);
      if (callDapperTDBridge(dataMoveDetail)) {
        JobResult jobResult = isDapperTaskComplete(dataMoveDetail.getTaskId());
        LOGGER.info("{} Step 2 Execute Result [{}]", dataMoveDetail, jobResult);
        if (jobResult.getStatus()) {
          if (Objects.nonNull(jobResult.getOutput())) {
            dataMoveDetail.getHistory().setStatus(2);
            dataMoveDetail.setErrorLog(jobResult.getOutput());
          } else {
            dataMoveDetail.setStep(3);
          }
        }
      }
    }

    return dataMoveDetail;
  }

  public DataMoveDetail runStepThree(DataMoveDetail dataMoveDetail) {
    if (dataMoveDetail.getHistory().getStatus() == 0 && dataMoveDetail.getStep() == 3) {
      LOGGER.info("{} Move Step 3", dataMoveDetail);
      String nT = dataMoveDetail.getHistory().getNt();
      Platform platform = Platform.valueOf(dataMoveDetail.getHistory().getTargetPlatform().trim());
      JobResult jobResult = hadoopTableOperation.executeSparkSQL(
          nT, platform, dataMoveDetail.getQueue(), getSparkSQL(dataMoveDetail));
      LOGGER.info("{} Step 3 Execute Result [{}]", dataMoveDetail, jobResult);
      if (jobResult.getStatus()) {
        if (StringUtils.isNoneBlank(dataMoveDetail.getHistory().getTouchFile())) {
          dataMoveDetail.setStep(4);
        } else {
          dataMoveDetail.getHistory().setStatus(1);
        }
      } else {
        dataMoveDetail.getHistory().setStatus(2);
        dataMoveDetail.setErrorLog(jobResult.getOutput());
      }
    }
    return dataMoveDetail;
  }

  public DataMoveDetail runStepFour(DataMoveDetail dataMoveDetail) {
    if (dataMoveDetail.getHistory().getStatus() == 0 && dataMoveDetail.getStep() == 4) {
      LOGGER.info("{} Move Step 4", dataMoveDetail);
      if (callDapperTouchFileGenerate(dataMoveDetail)) {
        JobResult jobResult = isDapperTaskComplete(dataMoveDetail.getTouchfileId());
        LOGGER.info("{} Step 4 Execute Result [{}]", dataMoveDetail, jobResult);
        if (jobResult.getStatus()) {
          if (Objects.nonNull(jobResult.getOutput())) {
            dataMoveDetail.getHistory().setStatus(2);
            dataMoveDetail.setErrorLog(jobResult.getOutput());
          } else {
            dataMoveDetail.getHistory().setStatus(1);
          }
        }
      }
    }
    return dataMoveDetail;
  }

  public boolean callDapperTDBridge(DataMoveDetail dataMoveDetail) {
    if (dataMoveDetail.getTaskId() == 0) {
      // check concurrent td bridge running number not over 8
      if (dataMoveRepository.getRunningDapperTaskByNT(dataMoveDetail.getHistory().getNt()) > 6) {
        LOGGER.warn("{} TD Bridge hold For Concurrent Restrict", dataMoveDetail);
        return false;
      }
      LOGGER.info("{} Kickoff TD-Bridge Task", dataMoveDetail);
      String sourcePlatform = dataMoveDetail.getHistory().getSourcePlatform().trim();
      String platformFrom = "numozart".equalsIgnoreCase(sourcePlatform) ? "mztlvs" : sourcePlatform;
      String platformTo = dataMoveDetail.getHistory().getTargetPlatform().trim();
      String[] tableInfo = dataMoveDetail.getHistory().getSourceTable().split("\\.");
      String hdBatchAccount = "dw_adm";
      String nt = dataMoveDetail.getHistory().getNt();
      String extraCmd = PropertiesUtil.getDatamoveProperties(EXTRA_CMD);
      String etlScript = PropertiesUtil.getDatamoveProperties(ETL_SCRIPT);

      String dataMoverShell = String.format(TD_BRIDGE_MOVE_SHELL,
          etlScript, platformFrom, platformTo, tableInfo[0],
          tableInfo[1], nt, getTdPwd(nt), hdBatchAccount,
          dataMoveDetail.getTdBridgeAvro(),
          dataMoveDetail.getQuery());
      if (StringUtils.isNotBlank(extraCmd)) {
        dataMoverShell = extraCmd + "\n" + dataMoverShell;
      }
      LOGGER.info("TD Bridge Shell CMD: {}", dataMoverShell);

      String etlServer = PropertiesUtil.getDatamoveProperties(ETL_SERVER);
      String jsonBody = String.format(
          "{\"steps\":[{\"uc4obj\":null,\"batchtasks\":[{\"shell\":\"ksh\","
              + "\"hosts\":[\"%s - dw_adm\"],\"steps\":[{\"commands\":\"%s\",\"comment\":\"\"}]}],"
              + "\"text_obj\":null}],\"ccMails\":\"\",\"isAutoExeStepOne\":true,\"existRaisePriority\":false}",
          etlServer,
          dataMoverShell);
      LOGGER.info("TD Bridge JsonBody: {}", jsonBody);
      int taskId = createDapperTask(jsonBody);
      if (taskId == -1) {
        return false;
      }
      dataMoveDetail.setTaskId(taskId);
    }
    return true;
  }

  public boolean callDapperTouchFileGenerate(DataMoveDetail dataMoveDetail) {
    if (dataMoveDetail.getTouchfileId() == 0) {
      LOGGER.info("{} Kickoff TouchFile-Generator", dataMoveDetail);
      Map<String, String> touchFileJson = JsonUtil.fromJson(
          dataMoveDetail.getHistory().getTouchFile(),
          new TypeReference<Map<String, String>>() {
          });
      String touchFilePath = touchFileJson.get("path").replace("YYYYMMDD", DateUtil.currentDate("yyyyMMdd"));

      String touchFileShell = String.format(TOUCH_FILE_GENERATOR, touchFilePath);
      String jsonBody = String.format(
          "{\"steps\":[{\"uc4obj\":null,\"batchtasks\":[{\"shell\":\"ksh\","
              + "\"hosts\":[\"%s - dw_adm\"],\"steps\":[{\"commands\":\"%s\",\"comment\":\"\"}]}],"
              + "\"text_obj\":null}],\"ccMails\":\"\",\"isAutoExeStepOne\":true,\"existRaisePriority\":false}",
          touchFileJson.get("server").trim(), touchFileShell);
      int taskId = createDapperTask(jsonBody);
      if (taskId == -1) {
        return false;
      }
      dataMoveDetail.setTouchfileId(taskId);
    }
    return true;
  }

  public int createDapperTask(String jsonBody) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("username", "ZetaDevSuite");
    params.add("jsonBody", jsonBody);
    HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(params, headers);
    ResponseEntity<Map> responseEntity = restTemplate.postForEntity(DAPPER_CREATE_TASK, requestEntity, Map.class);
    if (responseEntity.getStatusCode() == HttpStatus.OK) {
      Map<String, Object> task = responseEntity.getBody();
      if ((boolean) task.get("success")) {
        return (int) task.get("id");
      } else {
        return -1;
      }
    }
    return -1;
  }

  public JobResult isDapperTaskComplete(int taskId) {
    String dapperCheckURL = String.format(DAPPER_STATUS_CHECK, taskId, "ZetaDevSuite");
    LOGGER.info("Check Dapper task finished {}", dapperCheckURL);
    ResponseEntity<String> responseEntity = restTemplate.getForEntity(dapperCheckURL, String.class);
    if (responseEntity.getStatusCode() == HttpStatus.OK) {
      Object document = Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS).jsonProvider().parse(responseEntity.getBody());
      String taskStatus = JsonPath.read(document, "$.task_status");
      if ("Succeeded".equals(taskStatus)) {
        LOGGER.info("Dapper task [{}]-- finished successfully!", String.format(DAPPER_TASK_SHOW, taskId));
        return new JobResult(true);
      } else if ("Cancel".equals(taskStatus) || "Failed".equals(taskStatus)) {
        LOGGER.error(
            "Kickoff Td-Bridge failed, dapper task id is [{}]", String.format(DAPPER_TASK_SHOW, taskId));
        return new JobResult(true, JsonPath.read(document, "$.task_steps[0].step_batchTasks[0].batchTask_steps[0].batchStep_logs"));
      } else {
        LOGGER.info("Dapper task [{}]-- is not finished yet...", String.format(DAPPER_TASK_SHOW, taskId));
        return new JobResult(false);
      }
    }
    return new JobResult(false);
  }

  public String getTdPwd(String nt) {
    try {
      if (zetaUserService.getUser(nt).getTdPass() == null
          || zetaUserService.getUser(nt).getTdPass().trim().length() == 0) {
        throw new ToolSetCheckException("Please setup tetadata password!");
      } else {
        return Base64.getEncoder().encodeToString(
            (zetaUserService.getUser(nt).getTdPass().trim() + "+datamovement").getBytes("utf-8"));
      }
    } catch (UnsupportedEncodingException e) {
      throw new ToolSetCheckException("Teradata pwd can't be encodeed!");
    }
  }
}
