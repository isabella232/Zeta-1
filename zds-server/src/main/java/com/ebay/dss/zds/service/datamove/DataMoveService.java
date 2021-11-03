package com.ebay.dss.zds.service.datamove;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import com.ebay.dss.zds.common.JsonUtil;
import com.ebay.dss.zds.common.PropertiesUtil;
import com.ebay.dss.zds.exception.FileStorgeException;
import com.ebay.dss.zds.model.*;
import com.ebay.dss.zds.service.MailService;
import com.ebay.dss.zds.service.ZetaUserService;
import com.ebay.dss.zds.service.filehandler.CSVFileHandler;
import com.ebay.dss.zds.service.filehandler.FileHandler;
import com.ebay.dss.zds.service.filehandler.ParquetFileHandler;
import com.ebay.dss.zds.service.filestorage.FileStorageService;
import com.ebay.dss.zds.service.filestorage.FileType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ebay.dss.zds.dao.DataMoveRepository;
import com.ebay.dss.zds.dao.HistoryRepository;
import com.ebay.dss.zds.exception.ToolSetCheckException;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import static com.ebay.dss.zds.service.datamove.DataMoveJobType.LC2HD;
import static com.ebay.dss.zds.service.datamove.DataMoveJobType.VDM2HD;


/**
 * Created by zhouhuang on Apr 26, 2018
 */
@Service
@Transactional
public class DataMoveService {

  private final static Logger LOGGER = LoggerFactory.getLogger(DataMoveService.class);

  @Autowired
  private HistoryRepository historyRepository;

  @Autowired
  private DataMoveRepository dataMoveRepository;

  @Autowired
  private ZetaUserService zetaUserService;

  @Autowired
  private TableMetaService tableMetaService;

  @Autowired
  @Qualifier("LOCAL")
  private FileStorageService fileStorageService;

  @Autowired
  private WSMService wsmService;

  @Autowired
  private MailService mailService;


  private static final String DATAMOVE_UPLOAD_FOLDER = "upload.folder";

  public List<Map<String, Object>> getHistory(String nt) {
    List<Map<String, Object>> historyList = historyRepository.getHistoryList(nt, 1);
    historyList.addAll(historyRepository.getHistoryList(nt, 3));
    historyList.addAll(historyRepository.getHistoryList(nt, 4));
    historyList.addAll(historyRepository.getVdmViewHistoryList(nt));
    return historyList;
  }

  public ZetaResponse<Object> save(DataMoveDetail dataMoveDetail, DataMoveJobType dataMoveJobType) {
    dataMoveDetail.getHistory().setType(dataMoveJobType.getId());
    DataMoveJobVisitor dataMoveJobVisitor;
    switch (dataMoveJobType) {
      case TD2HD:
        dataMoveJobVisitor = new TD2HDJobVisitor();
        break;
      case LC2HD:
        dataMoveJobVisitor = new LC2HDJobVisitor();
        break;
      case VDM2HD:
      case VDMVIEW2HD:
        dataMoveJobVisitor = new VDM2HDJobVisitor();
        break;
      default:
        throw new ToolSetCheckException(String.format("Data move not support %s", dataMoveJobType.name()));
    }
    dataMoveJobVisitor.visit(dataMoveDetail);
    return new ZetaResponse<>("Success", HttpStatus.OK);
  }

  public Table getColumns(String platform, String dbName, String tblName) {
    return tableMetaService.getTableSchema(platform, dbName, tblName, TableMetaService.MetadataType.REALTIME);
  }

  public History getHistoryDetail(Long historyId) {
    return historyRepository.getHistoryByHistoryId(historyId);
  }

  public FileStorageResponse uploadFile(MultipartFile file, String nt) {
    if (file.isEmpty()) {
      throw new FileStorgeException(String.format("File %s is empty", file.getOriginalFilename()));
    }
    try {
      String uploadPath = String.format(PropertiesUtil.getDatamoveProperties(DATAMOVE_UPLOAD_FOLDER), nt);
      LOGGER.info("Data Move Upload Path: {}", uploadPath);
      String originFileName = file.getOriginalFilename();
      String fileName = originFileName.startsWith("_") ?
          originFileName.replaceFirst("_", "") : originFileName;
      FileStorage fileStorage = new FileStorage(fileName, uploadPath, nt);
      fileStorage.setFile(file.getInputStream());
      FileStorageResponse fileStorageResponse = fileStorageService.upload(fileStorage);
      fileStorageResponse.setContent(fileStorage.getFullPath());
      return fileStorageResponse;
    } catch (IOException e) {
      LOGGER.error("Read File Failed!", e);
      throw new FileStorgeException("Upload Failed", e);
    }
  }

  public Map<String, String> parseHeader(String fullPath) {
    FileHandler fileParser;
    switch (FileType.getFileTypeByPath(fullPath)) {
      case PARQUET:
        fileParser = new FileHandler(new ParquetFileHandler());
        break;
      case CSV:
        fileParser = new FileHandler(new CSVFileHandler());
        break;
      default:
        throw new FileStorgeException(String.format("File Type %s is not support",
            FileType.getFileTypeByPath(fullPath)));
    }
    return fileParser.executeParseHeader(fullPath);
  }

  public Map<String, Object> listFile(String nt) {
    FileStorage fileStorage = new FileStorage();
    fileStorage.setFullPath(String.format(PropertiesUtil.getDatamoveProperties(DATAMOVE_UPLOAD_FOLDER), nt));
    return fileStorageService.listFileName(fileStorage);
  }

  @Deprecated
  public List<String> getVDMWorkspaces(String nt, String platform) {
    checkParametersNotEmpty(ImmutableMap.of("platform", platform));
    return wsmService.getWorkspaces(platform.trim(), nt);
  }

  public List<String> getVDMDatabases(String nt, String platform, boolean isRealTime) {
    checkParametersNotEmpty(ImmutableMap.of("platform", platform));
    return wsmService.getDatabases(platform.trim(), nt, isRealTime);
  }

  public List<String> getVDMTables(String platform, String database, boolean isRealTime) {
    checkParametersNotEmpty((ImmutableMap.of(
        "platform", platform
        , "database", database)));
    return wsmService.getTablesInDB(platform.trim(), database.trim(), isRealTime);
  }

  private void checkParametersNotEmpty(Map<String, String> parameters) {
    for (Map.Entry<String, String> entry : parameters.entrySet()) {
      if (StringUtils.isBlank(entry.getValue())) {
        throw new ToolSetCheckException(String.format("%s is empty", entry.getKey()));
      }
    }
  }

  public ZetaResponse<Object> retryVDM2HDJob(String historyId) {
    DataMoveDetail dataMoveDetail = dataMoveRepository
        .findByHistory_HistoryId(Long.parseLong(historyId));
    int taskId = wsmService.retryVDMJob(dataMoveDetail.getTaskId());
    if (taskId == 0) {
      throw new ToolSetCheckException("VDM Retry failed.");
    }
    dataMoveDetail.setTaskId(taskId);
    dataMoveDetail.getHistory().setStatus(0);
    dataMoveDetail.getHistory().setLog(null);
    dataMoveDetail.getHistory().setStartTime(new Date());
    dataMoveDetail.getHistory().setEndTime(null);
    dataMoveRepository.save(dataMoveDetail);
    return new ZetaResponse<>("Success", HttpStatus.OK);
  }

  public List<String> validateVDM2HDJob(DataMoveDetail dataMoveDetail) {
    dataMoveDetail.getHistory().setType(VDM2HD.getId());
    return getOverrideTables(dataMoveDetail);
  }

  public List<String> getOverrideTables(DataMoveDetail dataMoveDetail) {
    if (DataMoveJobType.idOf(dataMoveDetail.getHistory().getType()) != VDM2HD) {
      return Lists.newArrayList();
    }
    Object response = wsmService.validateVDMTable(dataMoveDetail);
    LOGGER.info("Validate HDM Table Response: {}", response);
    List<String> overrideTables = JsonUtil.fromJson(
        response.toString(), new TypeReference<List<String>>() {
        });
    return overrideTables;
  }

  private String getVdmSourceDb(DataMoveDetail dataMoveDetail) {
    List<String> sourceTables = JsonUtil.fromJson(
        dataMoveDetail.getHistory().getSourceTable(),
        new TypeReference<List<String>>() {
        });
    return tableMetaService.getDBName(sourceTables.get(0));
  }

  public void sendVdmMoveOverrideEmail(DataMoveDetail dataMoveDetail
      , List<String> overrideTables) {
    try {
      LOGGER.info("Override tables: {}", overrideTables);
      String sourceDb = getVdmSourceDb(dataMoveDetail);
      LOGGER.info("Vdm source db: {}", sourceDb);
      // check if need to send override email
      if (DataMoveJobType.idOf(dataMoveDetail.getHistory().getType()) != VDM2HD
          || CollectionUtils.isEmpty(overrideTables)) {
        return;
      }
      mailService.sendVdmMoveOverrideEmail(dataMoveDetail, sourceDb, overrideTables);
    } catch (Exception e) {
      LOGGER.error("Send email failed", e);
    }
  }

  public List<String> getVDMViews(String nt, String platform, String database) {
    checkParametersNotEmpty((ImmutableMap.of(
        "platform", platform
        , "database", database)));
    return wsmService.getViewsInDB(nt, platform.trim(), database.trim());
  }

  interface DataMoveJobElement {
    void accept(DataMoveDetail dataMoveDetail);
  }

  class CheckTDPWD implements DataMoveJobElement {

    @Override
    public void accept(DataMoveDetail dataMoveDetail) {
      String nt = dataMoveDetail.getHistory().getNt().trim();
      if (zetaUserService.getUser(nt).getTdPass() == null
          || zetaUserService.getUser(nt).getTdPass().trim().length() == 0) {
        throw new ToolSetCheckException("Please setup tetadata password in User Settings Page!");
      }
    }
  }

  class CheckTDTableExist implements DataMoveJobElement {

    @Override
    public void accept(DataMoveDetail dataMoveDetail) {
      String dbName = tableMetaService.getDBName(dataMoveDetail.getHistory().getSourceTable());
      String tableName = tableMetaService.getTableName(dataMoveDetail.getHistory().getSourceTable());
      LOGGER.info("GET Source Table DB name is [{}], table name is [{}]", dbName, tableName);
      if (Objects.isNull(dbName) || Objects.isNull(tableName)) {
        throw new ToolSetCheckException("DB or Table name can't be empty!");
      }
      tableMetaService.getTableSchema(
          dataMoveDetail.getHistory().getSourcePlatform().trim(), dbName, tableName,
          TableMetaService.MetadataType.DAILY);
    }
  }

  class CheckFileSchema implements DataMoveJobElement {

    @Override
    public void accept(DataMoveDetail dataMoveDetail) {
      if (MapUtils.isEmpty(JsonUtil.fromJson(dataMoveDetail.getDdl(),
          new TypeReference<Map<String, String>>() {
          }))) {
        throw new ToolSetCheckException("File Schema is Empty!");
      }
    }
  }

  class CheckJobRunning implements DataMoveJobElement {
    @Override
    public void accept(DataMoveDetail dataMoveDetail) {
      if (historyRepository.findBySourceTableAndSourcePlatformAndStatusAndTypeAndNt(
          dataMoveDetail.getHistory().getSourceTable(),
          dataMoveDetail.getHistory().getSourcePlatform(),
          0, dataMoveDetail.getHistory().getType(),
          dataMoveDetail.getHistory().getNt()).size() > 0) {
        if (DataMoveJobType.idOf(dataMoveDetail.getHistory().getType()).equals(LC2HD)) {
          throw new ToolSetCheckException(
              String.format("There exists job with the same file name [%s] is running. " +
                      "Please wait for the job done or change a new file name.",
                  dataMoveDetail.getHistory().getSourceTable()));
        } else {
          throw new ToolSetCheckException(
              String.format("There exists job with the same source table [%s] in [%s] is running. " +
                      "Please wait for the job done.",
                  dataMoveDetail.getHistory().getSourceTable(),
                  dataMoveDetail.getHistory().getSourcePlatform()));
        }
      }
    }
  }

  @Deprecated
  class CheckVDMSourceDb implements DataMoveJobElement {

    @Override
    public void accept(DataMoveDetail dataMoveDetail) {
      String dbName = tableMetaService.getDBName(dataMoveDetail.getHistory().getSourceTable());
      LOGGER.info("VDM source platform: {}", dataMoveDetail.getHistory().getVDMSourcePlatform());
      List<String> dbList = wsmService.getDatabases(
          dataMoveDetail.getHistory().getVDMSourcePlatform(), dataMoveDetail.getHistory().getNt(), true);
      if (Objects.isNull(dbName) || (!dbList.contains(dbName.toLowerCase()))) {
        throw new ToolSetCheckException(String.format("You don't have access to VDM database %s", dbName));
      }
    }
  }

  @Deprecated
  class CheckVDMSourceTable implements DataMoveJobElement {

    @Override
    public void accept(DataMoveDetail dataMoveDetail) {
      String dbName = tableMetaService.getDBName(dataMoveDetail.getHistory().getSourceTable());
      String tableName = tableMetaService.getTableName(dataMoveDetail.getHistory().getSourceTable());
      List<String> tableList = wsmService.getTablesInDB(dataMoveDetail.getHistory().getVDMSourcePlatform(),
          dbName, true);
      if (Objects.isNull(tableName) || (!tableList.contains(tableName.toLowerCase()))) {
        throw new ToolSetCheckException(String.format("You don't have access to VDM table %s", tableName));
      }
    }
  }

  @Deprecated
  class CheckVDMTargetDb implements DataMoveJobElement {

    @Override
    public void accept(DataMoveDetail dataMoveDetail) {
      String dbName = tableMetaService.getDBName(dataMoveDetail.getHistory().getTargetTable());
      List<String> workspaceList = wsmService.getWorkspaces(
          dataMoveDetail.getHistory().getTargetPlatform(), dataMoveDetail.getHistory().getNt());
      LOGGER.info("Get {} available workspace list: {}", dataMoveDetail.getHistory().getNt(), workspaceList);
      if (Objects.isNull(dbName) || (!workspaceList.contains(dbName.toLowerCase()))) {
        String errorMsg = workspaceList.isEmpty() ?
            String.format("You don't have access to VDM workspace %s. " +
                    "Please contact us to create workspace in %s.",
                dbName, dataMoveDetail.getHistory().getTargetPlatform())
            : String.format("You don't have access to VDM workspace %s. " +
                "You can use below workspace you can access.\n%s",
            dbName, workspaceList);
        throw new ToolSetCheckException(errorMsg);
      }
    }
  }

  class CheckVDMSourceTableNotEmpty implements DataMoveJobElement {

    @Override
    public void accept(DataMoveDetail dataMoveDetail) {
      List<String> sourceTables = JsonUtil.fromJson(dataMoveDetail.getHistory().getSourceTable(),
          new TypeReference<List<String>>() {
          });
      if (CollectionUtils.isEmpty(sourceTables)) {
        throw new ToolSetCheckException("Source Table can't be empty.");
      }
    }

  }

  class CheckVDMDuplicateSourceTable implements DataMoveJobElement {
    @Override
    public void accept(DataMoveDetail dataMoveDetail) {
      List<String> sourceTables = JsonUtil.fromJson(dataMoveDetail.getHistory().getSourceTable(),
          new TypeReference<List<String>>() {
          });
      dataMoveDetail.getHistory().setSourceTable(
          JsonUtil.toJson(sourceTables.stream().distinct().collect(Collectors.toList()))
      );
    }
  }

  class SaveJob implements DataMoveJobElement {

    @Override
    public void accept(DataMoveDetail dataMoveDetail) {
      dataMoveDetail.getHistory().setCreateDate(new Date());
      dataMoveDetail.getHistory().setLastRunDate(new Date());
      dataMoveDetail.setCreateDate(new Date());
      dataMoveDetail.setStep(0);
      dataMoveRepository.save(dataMoveDetail);
    }
  }

  abstract class DataMoveJobVisitor {
    DataMoveJobElement[] elements;

    void visit(DataMoveDetail dataMoveDetail) {
      for (DataMoveJobElement element : elements) {
        element.accept(dataMoveDetail);
      }

    }
  }

  class TD2HDJobVisitor extends DataMoveJobVisitor {

    TD2HDJobVisitor() {
      elements = new DataMoveJobElement[]{
          new CheckTDPWD()
          , new CheckTDTableExist()
          , new CheckJobRunning()
          , new SaveJob()};
    }
  }

  class LC2HDJobVisitor extends DataMoveJobVisitor {

    LC2HDJobVisitor() {
      elements = new DataMoveJobElement[]{
          new CheckFileSchema()
          , new CheckJobRunning()
          , new SaveJob()};
    }
  }

  class VDM2HDJobVisitor extends DataMoveJobVisitor {

    VDM2HDJobVisitor() {
      elements = new DataMoveJobElement[]{
          new CheckVDMSourceTableNotEmpty()
          , new CheckVDMDuplicateSourceTable()
          , new SaveJob()};
    }
  }

}