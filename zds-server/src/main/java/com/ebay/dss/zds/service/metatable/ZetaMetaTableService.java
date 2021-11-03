package com.ebay.dss.zds.service.metatable;


import com.ebay.dss.zds.common.BeanUtils;
import com.ebay.dss.zds.common.JsonUtil;
import com.ebay.dss.zds.dao.ZetaMetaTableRepository;
import com.ebay.dss.zds.exception.ApplicationBaseException;
import com.ebay.dss.zds.exception.ErrorCode;
import com.ebay.dss.zds.exception.MetaTableException;
import com.ebay.dss.zds.model.Platform;
import com.ebay.dss.zds.model.ZetaResponse;
import com.ebay.dss.zds.model.authorization.AuthorizationInfo;
import com.ebay.dss.zds.model.authorization.AuthorizationInfo.AuthType;
import com.ebay.dss.zds.model.authorization.AuthorizationMail;
import com.ebay.dss.zds.model.metatable.MetaTableOperation;
import com.ebay.dss.zds.model.metatable.MetaTableOperation.Operations;
import com.ebay.dss.zds.model.metatable.MetaTableOperation.TableSchemaInfo;
import com.ebay.dss.zds.model.metatable.ZetaMetaTable;
import com.ebay.dss.zds.service.MailService;
import com.ebay.dss.zds.service.authorization.ZetaAuthorizationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.jsonwebtoken.lang.Assert;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import static com.ebay.dss.zds.common.JsonUtil.MAPPER;
import static com.ebay.dss.zds.model.metatable.MetaTableOperation.Operations.*;
import static com.ebay.dss.zds.model.metatable.ZetaMetaTable.MetaTableStatus.*;
import static com.ebay.dss.zds.model.metatable.ZetaMetaTable.MetaTableStatus.SUCCESS;
import static com.ebay.dss.zds.service.MailService.*;
import static com.ebay.dss.zds.service.authorization.AuthorizationConstant.*;
import static com.ebay.dss.zds.service.authorization.AuthorizationType.ZETA_SHEET;
import static com.ebay.dss.zds.service.metatable.MetaTableConstant.*;
import static com.ebay.dss.zds.service.metatable.MetaTableResponse.*;
import static com.ebay.dss.zds.service.metatable.OLAPMetaTableReceiver.TableField._ID_;
import static com.ebay.dss.zds.service.metatable.OLAPMetaTableReceiver.TableField._VERSION_;


@Service
public class ZetaMetaTableService {

  private static final Logger LOGGER = LoggerFactory.getLogger(ZetaMetaTableService.class);

  @Autowired
  private ZetaAuthorizationService metaTableAuthorization;

  @Autowired
  private ZetaMetaTableRepository zetaMetaTableRepository;

  @Autowired
  private MailService mailService;

  @Resource(name = "metaTableTaskExecutor")
  private Executor executor;

  @Autowired
  private OLAPMetaTableReceiver olapMetaTableReceiver;

  @Autowired
  private LoggerReceiver loggerReceiver;

  @Autowired
  private HadoopMetaTableReceiver hadoopMetaTableReceiver;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Resource(name = "resttemplate")
  private RestTemplate restTemplate;

  private OperationInvoker invoker = new OperationInvoker();

  private CreateCommand createCommand;

  private DropCommand dropCommand;

  private UpdateCommand updateCommand;

  private UpdateCommand syncCommand;

  private CreateCommand registerCommand;

  private QueryCommand queryCommand;

  private static String PUSH_SCHEMA_URL;

  @Value("${doe.new.url}")
  private void initServiceUrl(String hostUrl) {
//    zeta-sheet.pushschema.url=http://opsins-service.dss-doe.svc.57.tess.io/asset/addColDesc
    PUSH_SCHEMA_URL = hostUrl + "/vdm/registerZetaSheet";
  }

  @PostConstruct
  public void initCommand() {
    LOGGER.info("Init Command");
    initUpdateCommand();
    initDropCommand();
    initSyncCommand();
    initRegisterCommand();
    this.createCommand = new CreateCommand(olapMetaTableReceiver);
    this.queryCommand = new QueryCommand(olapMetaTableReceiver);
  }

  private void initUpdateCommand() {
    UpdateCommand olapCommand = new UpdateCommand(olapMetaTableReceiver);
    UpdateCommand loggerCommand = new UpdateCommand(loggerReceiver);
    olapCommand.setNextHandler(loggerCommand);
    this.updateCommand = olapCommand;
  }

  private void initDropCommand() {
    DropCommand olapCommand = new DropCommand(olapMetaTableReceiver);
    DropCommand loggerCommand = new DropCommand(loggerReceiver);
    olapCommand.setNextHandler(loggerCommand);
    this.dropCommand = olapCommand;
  }

  private void initSyncCommand() {
    UpdateCommand hadoopCommand = new UpdateCommand(hadoopMetaTableReceiver);
    UpdateCommand loggerCommand = new UpdateCommand(loggerReceiver);
    hadoopCommand.setNextHandler(loggerCommand);
    this.syncCommand = hadoopCommand;
  }

  private void initRegisterCommand() {
    CreateCommand hadoopCommand = new CreateCommand(hadoopMetaTableReceiver);
    CreateCommand loggerCommand = new CreateCommand(loggerReceiver);
    hadoopCommand.setNextHandler(loggerCommand);
    this.registerCommand = hadoopCommand;
  }

  public List<ZetaMetaTable> getZetaMetaTableList(String nt) {
    LOGGER.info("Get {} Zeta Sheet List", nt);
    List<ZetaMetaTable> zetaMetaTableList = zetaMetaTableRepository
        .findByMetaTableStatusNotOrderByUpdateTimeDesc(DELETED);
    return zetaMetaTableList.stream().filter(zetaMetaTable ->
        metaTableAuthorization.isReader(nt, zetaMetaTable)).collect(Collectors.toList());
  }

  public ZetaResponse createZetaMetaTable(String nt, ZetaMetaTable zetaMetaTable) {
    Assert.notNull(zetaMetaTable, "MetaTable mustn't be null");
    try {
      checkMetaTableNameNotExist(zetaMetaTable.getMetaTableName(), nt);
      checkTableSchemaInfo(zetaMetaTable);
      /* check hadoop info
      checkPlatformExist(zetaMetaTable.getPlatform());
      checkTableNameNotExist(zetaMetaTable.getDb(), zetaMetaTable.getTbl(), zetaMetaTable.getPlatform());
       */
      initZetaMetaTable(nt, zetaMetaTable);
      invoker.action(createCommand, zetaMetaTable);
      zetaMetaTable.setMetaTableStatus(CREATED);
      zetaMetaTableRepository.save(zetaMetaTable);
    } catch (RuntimeException e) {
      return new ZetaResponse<>(
          new MetaTableResponse<>(FAIL_CODE, e.getMessage()), HttpStatus.OK);
    }

    return new ZetaResponse<>(
        new MetaTableResponse<>(SUCCESS_CODE, zetaMetaTable.getId()), HttpStatus.OK);
  }

  private void checkPlatformExist(String platform) {
    try {
      Platform.valueOf(platform);
    } catch (IllegalArgumentException e) {
      throw new MetaTableException(String.format("Platform [%s] is not exist", platform));
    }
  }

  private void checkMetaTableNameNotExist(String metaTableName, String nt) {
    if (Objects.nonNull(metaTableName)) {
      zetaMetaTableRepository.findByMetaTableNameAndNtAndMetaTableStatusNot(metaTableName, nt, DELETED)
          .ifPresent(obj -> {
            throw new MetaTableException(String.format("MetaTableName [%s] is exist", metaTableName));
          });
    }
  }


  private void checkTableSchemaInfo(ZetaMetaTable zetaMetaTable) {
    List<TableSchemaInfo> schemaList = JsonUtil.fromJson(zetaMetaTable.getSchemaInfo()
        , new TypeReference<List<TableSchemaInfo>>() {
        });
    Assert.notEmpty(schemaList);
    long distinctSize = schemaList.stream().map(TableSchemaInfo::getColumn)
        .map(String::toLowerCase).distinct().count();
    if (distinctSize < schemaList.size()) {
      throw new MetaTableException(DUPLICATE_COLUMN);
    }
    checkColumnSchemaInfo(schemaList);
    zetaMetaTable.setSchemaInfo(JsonUtil.toJson(schemaList));
  }

  private void checkColumnSchemaInfo(List<TableSchemaInfo> schemaList) {
    for (TableSchemaInfo schema : schemaList) {
      switch (ColumnType.valueOf(schema.getType().toUpperCase())) {
        case VARCHAR:
          if (isColumnLengthEmpty(schema)) {
            schema.setLength("255");
          }
          break;
        case INT:
          if (isColumnLengthEmpty(schema)) {
            schema.setLength("11");
          }
          break;
        case BIGINT:
          if (isColumnLengthEmpty(schema)) {
            schema.setLength("20");
          }
          break;
        case DOUBLE:
        case DECIMAL:
          if (isColumnLengthEmpty(schema)) {
            schema.setLength("16,4");
          } else {
            List<String> lens = Splitter.on(",").splitToList(schema.getLength());
            if (lens.size() < 2) {
              throw new MetaTableException(
                  "For float, double or decimal, the length format is M,D (M must be >= D)");
            }
            if (Integer.parseInt(lens.get(0)) < Integer.parseInt(lens.get(1))) {
              throw new MetaTableException(String.format(
                  "For float(M,D), double(M,D) or decimal(M,D), M must be >= D (column '%s')"
                  , schema.getColumn()));
            }
          }
          break;
      }
    }
  }

  private boolean isColumnLengthEmpty(TableSchemaInfo schema) {
    return StringUtils.isBlank(schema.getLength()) || "0".equals(schema.getLength());
  }

  private void initZetaMetaTable(String nt, ZetaMetaTable zetaMetaTable) {
    zetaMetaTable.setId(UUID.randomUUID().toString());
    zetaMetaTable.setNt(nt);
    zetaMetaTable.setAuthInfo(JsonUtil.toJson(metaTableAuthorization.initAuthInfo()));
    zetaMetaTable.setCreateTime(new Date());
    zetaMetaTable.setUpdateTime(new Date());
  }

  public ZetaResponse updateZetaMetaTable(String nt, String id
      , MetaTableOperation metaTableOperation) {

    ZetaMetaTable zetaMetaTable = findMetaTableById(id);
    if (!metaTableAuthorization.isOwner(nt, zetaMetaTable)) {
      return new ZetaResponse<>(
          new MetaTableResponse<>(FORBIDDEN_CODE, NO_ACCESS_UPDATE), HttpStatus.OK);
    }
    try {
      // update meta table info
      if (Objects.nonNull(metaTableOperation.getMetaTableInfo())) {
        ZetaMetaTable updatedMetaTable = MAPPER.convertValue(
            metaTableOperation.getMetaTableInfo(), ZetaMetaTable.class);
        checkMetaTableNameNotExist(updatedMetaTable.getMetaTableName(), nt);
        BeanUtils.merge(zetaMetaTable, updatedMetaTable, UPDATE_TABLE_FIELDS);
        /*
        if (Objects.nonNull(updatedMetaTable.getCron())) {
          // update scheduler
          invoker.action(updateScheduleCommand, zetaMetaTable, null);
        }
        */
      }
      // update meta table schema
      if (Objects.nonNull(metaTableOperation.getOperations())) {
        checkTableOperationSchema(metaTableOperation.getOperations(), zetaMetaTable);
        zetaMetaTable.setLastModifier(nt);
        invoker.action(updateCommand, zetaMetaTable, metaTableOperation);
      }
    } catch (MetaTableException e) {
      return new ZetaResponse<>(
          new MetaTableResponse<>(FAIL_CODE, e.getMessage()), HttpStatus.OK);
    } catch (RuntimeException e) {
      updateZetaMetaTableRepo(zetaMetaTable);
      return new ZetaResponse<>(
          new MetaTableResponse<>(FAIL_CODE, e.getMessage()), HttpStatus.OK);
    }
    updateZetaMetaTableRepo(zetaMetaTable);
    return new ZetaResponse<>(MetaTableResponse.SUCCESS, HttpStatus.OK);
  }

  private void checkTableOperationSchema(Map<Operations, List<Object>> operations
      , ZetaMetaTable zetaMetaTable) {
    if ((!operations.containsKey(ADD)) || operations.get(ADD).isEmpty()) {
      return;
    }
    // add schema
    List<TableSchemaInfo> operationSchema = operations.get(ADD).stream()
        .map(this::getColumnInfo)
        .collect(Collectors.toList());
    long distinctSize = operationSchema.stream().map(TableSchemaInfo::getColumn)
        .map(String::toLowerCase).distinct().count();
    // drop column
    List<String> dropColumns = operations.getOrDefault(DROP, Lists.newArrayList()).stream()
        .map(this::getColumnInfo)
        .map(obj -> obj.getColumn().toLowerCase()).collect(Collectors.toList());
    // table schema
    List<TableSchemaInfo> schemaList = JsonUtil.fromJson(zetaMetaTable.getSchemaInfo()
        , new TypeReference<List<TableSchemaInfo>>() {
        });
    // table column
    List<String> columns = schemaList.stream().map(obj -> obj.getColumn().toLowerCase())
        .collect(Collectors.toList());
    if (distinctSize < operationSchema.size() || operationSchema.stream().anyMatch(schema ->
        columns.contains(schema.getColumn().toLowerCase())
            && (!dropColumns.contains(schema.getColumn().toLowerCase())))) {
      throw new MetaTableException(DUPLICATE_COLUMN);
    }
    checkColumnSchemaInfo(operationSchema);
    operations.put(ADD, operationSchema.stream()
        .map(obj -> MAPPER.convertValue(obj, Object.class))
        .collect(Collectors.toList()));
  }

  private TableSchemaInfo getColumnInfo(Object obj) {
    TableSchemaInfo tableSchemaInfo = MAPPER.convertValue(obj, TableSchemaInfo.class);
    if (Objects.nonNull(tableSchemaInfo.getColumn())
        && StringUtils.isNumericSpace(tableSchemaInfo.getColumn())) {
      throw new MetaTableException("Column name can't be Number");
    }
    return tableSchemaInfo;
  }

  private void updateZetaMetaTableRepo(ZetaMetaTable zetaMetaTable) {
    zetaMetaTable.setUpdateTime(new Date());
    zetaMetaTableRepository.save(zetaMetaTable);
  }

  @Transactional
  public synchronized void updateZetaMetaTablePath(String nt, List<ZetaMetaTable> metaTables) {
    LOGGER.info("{} update path for {}", nt, JsonUtil.toJson(metaTables));
    metaTables.stream().forEach(metaTable -> updateZetaMetaTablePath(nt, metaTable));
  }

  private void updateZetaMetaTablePath(String nt, ZetaMetaTable metaTable) {
    Assert.hasText(metaTable.getId(), "Notebook ID is NULL");
    Assert.hasText(metaTable.getPath(), "Notebook Path is NULL");
    ZetaMetaTable zetaMetaTable = findMetaTableById(metaTable.getId());
    Map paths = JsonUtil.fromJson(
        Optional.ofNullable(zetaMetaTable.getPath()).orElse("{}"), Map.class);
    paths.put(nt, metaTable.getPath());
    zetaMetaTableRepository.updateTablePath(metaTable.getId(), JsonUtil.toJson(paths));
  }

  public ZetaResponse getSyncStatus(String nt, String id) {
    LOGGER.info("GET {} Sync Status for {}", id, nt);
    ZetaMetaTable zetaMetaTable = findMetaTableById(id);
    switch (zetaMetaTable.getMetaTableStatus()) {
      case REGISTER_FAIL:
        throw new ApplicationBaseException(ErrorCode.SHEET_SYNC_REGISTER_FAIL, zetaMetaTable.getFailLog());
      case LOAD_FAIL:
        throw new ApplicationBaseException(ErrorCode.SHEET_SYNC_LOAD_FAIL, zetaMetaTable.getFailLog());
    }
    return new ZetaResponse<>(
        new MetaTableResponse<>(SUCCESS_CODE, getSyncStatusObject(zetaMetaTable)), HttpStatus.OK);
  }

  private Map<String, Object> getSyncStatusObject(ZetaMetaTable zetaMetaTable) {
    Map<String, Object> metaTableStatus = Maps.newLinkedHashMap();
    metaTableStatus.put("metaTableStatus", zetaMetaTable.getMetaTableStatus());
    metaTableStatus.put("failLog", zetaMetaTable.getFailLog());
    metaTableStatus.put("syncTime", zetaMetaTable.getSyncTime());
    return metaTableStatus;
  }

  public synchronized ZetaResponse syncHadoopTable(String nt, String id, ZetaMetaTable registerTable) {
    ZetaMetaTable zetaMetaTable = findMetaTableById(id);
    if (!metaTableAuthorization.isOwner(nt, zetaMetaTable)) {
      return new ZetaResponse<>(
          new MetaTableResponse<>(FORBIDDEN_CODE, NO_ACCESS_OPERATE), HttpStatus.OK);
    }
    if (zetaMetaTable.getMetaTableStatus() == SYNCING) {
      return new ZetaResponse<>(
          new MetaTableResponse<>(FORBIDDEN_CODE, SYNCING_INFO), HttpStatus.OK);
    }
    Action action = Action.SYNC;
    try {
      if (Objects.nonNull(registerTable) &&
          Objects.isNull(zetaMetaTable.getPlatform()) &&
          Objects.isNull(zetaMetaTable.getDb()) &&
          Objects.isNull(zetaMetaTable.getTbl())) {
        registerHadoopTable(zetaMetaTable, registerTable);
        action = Action.REGISTER_SYNC;
      }
      zetaMetaTable.setMetaTableStatus(SYNCING);
      zetaMetaTable.setLastModifier(nt);
      updateZetaMetaTableRepo(zetaMetaTable);
      submitHadoopTableAction(zetaMetaTable, action);
    } catch (MetaTableException e) {
      return new ZetaResponse<>(
          new MetaTableResponse<>(FAIL_CODE, e.getMessage()), HttpStatus.OK);
    }
    return new ZetaResponse<>(
        new MetaTableResponse<>(SUCCESS_CODE, getSyncStatusObject(zetaMetaTable)), HttpStatus.OK);
  }


  private void registerHadoopTable(ZetaMetaTable originTable, ZetaMetaTable registerTable) {
    checkPlatformExist(registerTable.getPlatform());
    BeanUtils.merge(originTable, registerTable, REGISTER_TABLE_FIELDS);
  }

  private enum Action {
    REGISTER_SYNC, SYNC
  }

  private void submitHadoopTableAction(ZetaMetaTable zetaMetaTable, Action action) {
    executor.execute(() -> {
      ZetaMetaTable.MetaTableStatus metaTableStatus = SUCCESS;
      try {
        List<Map<String, Object>> loadData = (List<Map<String, Object>>)
            invoker.action(queryCommand, zetaMetaTable);
        loadData.forEach(data -> {
          data.remove(_ID_.name());
          data.remove(_VERSION_.name());
        });
        switch (action) {
          case REGISTER_SYNC:
            try {
              invoker.action(registerCommand, zetaMetaTable);
            } catch (Exception e) {
              clearZetaSheetHadoopTableInfo(zetaMetaTable);
              metaTableStatus = REGISTER_FAIL;
              throw new MetaTableException(e.getMessage());
            }
            invoker.action(syncCommand, zetaMetaTable, loadData);
            break;
          case SYNC:
            invoker.action(syncCommand, zetaMetaTable, loadData);
        }
        pushSchemaToMetadata(zetaMetaTable);
        zetaMetaTable.setSyncTime(new Date());
        zetaMetaTable.setHadoopSchemaInfo(zetaMetaTable.getSchemaInfo());
      } catch (Exception e) {
        zetaMetaTable.setFailLog(e.getMessage());
        metaTableStatus = metaTableStatus == SUCCESS ? LOAD_FAIL : metaTableStatus;
      } finally {
        zetaMetaTable.setMetaTableStatus(metaTableStatus);
        pushUpdateToZetaMetaTable(zetaMetaTable);
      }
    });
  }

  private void pushUpdateToZetaMetaTable(ZetaMetaTable zetaMetaTable) {
    ZetaMetaTable latestZetaMetaTable = findMetaTableById(zetaMetaTable.getId());
    BeanUtils.merge(latestZetaMetaTable, zetaMetaTable, PUSH_TABLE_FIELDS);
    zetaMetaTableRepository.save(latestZetaMetaTable);
  }

  private void pushSchemaToMetadata(ZetaMetaTable zetaMetaTable) {
    try {
      Map<String, Object> requestBody = getPushSchemaRequestBody(zetaMetaTable);
      LOGGER.info("Push schema request body: {}", JsonUtil.toJson(requestBody));
      ResponseEntity<Map> responseEntity = restTemplate.postForEntity(PUSH_SCHEMA_URL, requestBody, Map.class);
      LOGGER.info("Push schema to Metadata ResponseEntity: {}", responseEntity);
    } catch (Exception e) {
      LOGGER.error("Push schema to Metadata fail!", e);
    }
  }

  private Map<String, Object> getPushSchemaRequestBody(ZetaMetaTable zetaMetaTable) {
    Map<String, Object> requestBody = Maps.newLinkedHashMap();
    requestBody.put("platforms", getPushSchemaPlatform(zetaMetaTable.getPlatform()));
    requestBody.put("db", zetaMetaTable.getDb());
    requestBody.put("table", zetaMetaTable.getTbl());
    requestBody.put("owner_nt", zetaMetaTable.getNt());
    requestBody.put("table_desc", "");
    requestBody.put("cre_user", zetaMetaTable.getLastModifier());
    List<TableSchemaInfo> tableSchemaInfoList = JsonUtil.fromJson(zetaMetaTable.getSchemaInfo()
        , new TypeReference<List<TableSchemaInfo>>() {
        });
    List<Map> columnInfoList = tableSchemaInfoList.stream()
        .map(table -> genColumnInfo(zetaMetaTable, table)).collect(Collectors.toList());
    requestBody.put("columns", columnInfoList);
    return requestBody;

  }

  private String getPushSchemaPlatform(String platform) {
    return "apollorno".equalsIgnoreCase(platform) ? "apollo_rno" : platform;
  }

  private Map<String, String> genColumnInfo(ZetaMetaTable zetaMetaTable, TableSchemaInfo schemaInfo) {
    Map<String, String> column = Maps.newLinkedHashMap();
    column.put("column_name", schemaInfo.getColumn());
    column.put("column_desc", schemaInfo.getDesc());
    return column;
  }

  private void clearZetaSheetHadoopTableInfo(ZetaMetaTable zetaMetaTable) {
    zetaMetaTable.setPlatform(null);
    zetaMetaTable.setAccount(null);
    zetaMetaTable.setDb(null);
    zetaMetaTable.setTbl(null);
    zetaMetaTable.setMetaTableType(null);
  }

  public ZetaResponse deleteZetaMetaTable(String nt, String id) {

    ZetaMetaTable zetaMetaTable = findMetaTableById(id);
    if (!metaTableAuthorization.isOwner(nt, zetaMetaTable)) {
      return new ZetaResponse<>(
          new MetaTableResponse<>(FORBIDDEN_CODE, NO_ACCESS_DELETE), HttpStatus.OK);
    }
    if (zetaMetaTable.getMetaTableStatus().equals(CREATED)) {
      // remove table
      executor.execute(() -> {
        zetaMetaTable.setLastModifier(nt);
        invoker.action(dropCommand, zetaMetaTable);
      });
    }
    zetaMetaTable.setMetaTableStatus(DELETED);
    zetaMetaTableRepository.save(zetaMetaTable);
    return new ZetaResponse<>(MetaTableResponse.SUCCESS, HttpStatus.OK);
  }

  public ZetaResponse grantAuthInfo(String nt, String id
      , Map<AuthType, Set<AuthorizationInfo>> authInfo) {

    metaTableAuthorization.checkAuthInfoFormat(authInfo);
    ZetaMetaTable zetaMetaTable = findMetaTableById(id);
    if (!metaTableAuthorization.isOwner(nt, zetaMetaTable)) {
      return new ZetaResponse<>(
          new MetaTableResponse<>(FORBIDDEN_CODE, NO_ACCESS_GRANT), HttpStatus.OK);
    }

    if (zetaMetaTableRepository.updateMetaTableAuthInfo(
        JsonUtil.toJson(authInfo), id) < 1) {
      throw new MetaTableException(String.format(META_TABLE_NOT_EXIST, id));
    }

    // send email
    sendGrantMetaTableEmail(nt, zetaMetaTable, authInfo);
    return new ZetaResponse<>(MetaTableResponse.SUCCESS, HttpStatus.OK);
  }


  private void sendGrantMetaTableEmail(String nt, ZetaMetaTable zetaMetaTable
      , Map<AuthType, Set<AuthorizationInfo>> authInfo) {
    Map<AuthType, Set<AuthorizationInfo>> originAuthInfo =
        StringUtils.isBlank(zetaMetaTable.getAuthInfo()) ?
            metaTableAuthorization.initAuthInfo() :
            metaTableAuthorization.parseAuthInfo(zetaMetaTable.getAuthInfo());
    AuthorizationMail authorizationMail = new AuthorizationMail();
    authorizationMail.setJobName(zetaMetaTable.getMetaTableName());
    authorizationMail.setAuthorizationType(ZETA_SHEET);
    authorizationMail.setAuthInfo(authInfo);
    authorizationMail.setOriginAuthInfo(originAuthInfo);
    authorizationMail.setLink(META_TABLE_URL);
    metaTableAuthorization.sendGrantEmail(nt, authorizationMail);
  }


  private ZetaMetaTable findMetaTableById(String id) {
    /*
    return zetaMetaTableRepository.findByIdAndMetaTableStatusNot(id, DELETED)
        .orElseThrow(() -> new MetaTableException(META_TABLE_NOT_EXIST));
    */
    Optional<ZetaMetaTable> zetaMetaTable = zetaMetaTableRepository.findById(id);
    if (zetaMetaTable.isPresent() && zetaMetaTable.get().getMetaTableStatus() != DELETED) {
      return zetaMetaTable.get();
    } else {
      throw new MetaTableException(String.format(META_TABLE_NOT_EXIST
          , zetaMetaTable.isPresent() ? zetaMetaTable.get().getMetaTableName() : ""));
    }
  }


  public ZetaResponse getZetaMetaTableInfo(String nt, String id, boolean isPrivate) {
    ZetaMetaTable zetaMetaTable = findMetaTableById(id);
    if (isPrivate && !metaTableAuthorization.isReader(nt, zetaMetaTable)) {
      return new ZetaResponse<>(
          new MetaTableResponse<>(FORBIDDEN_CODE, NO_ACCESS_READ), HttpStatus.OK);
    }
    return new ZetaResponse<>(
        new MetaTableResponse<>(SUCCESS_CODE, getMetaTableInfo(zetaMetaTable)), HttpStatus.OK);
  }

  private Map getMetaTableInfo(ZetaMetaTable zetaMetaTable) {
    Map<String, Object> metaTableInfo = Maps.newLinkedHashMap();
    metaTableInfo.put("zetaMetaTable", zetaMetaTable);
    metaTableInfo.put("data", invoker.action(queryCommand, zetaMetaTable));
    return metaTableInfo;
  }


  public ZetaResponse editZetaMetaTable(String nt, String id
      , MetaTableOperation metaTableOperation) {
    ZetaMetaTable zetaMetaTable = findMetaTableById(id);
    if (!metaTableAuthorization.isWriter(nt, zetaMetaTable)) {
      return new ZetaResponse<>(
          new MetaTableResponse<>(FORBIDDEN_CODE, NO_ACCESS_EDIT), HttpStatus.OK);
    }
    try {
      zetaMetaTable.setLastModifier(nt);
      invoker.action(updateCommand, zetaMetaTable, metaTableOperation);
      updateZetaMetaTableRepo(zetaMetaTable);
    } catch (RuntimeException e) {
      return new ZetaResponse<>(
          new MetaTableResponse<>(FAIL_CODE, e.getMessage()), HttpStatus.OK);
    }
    return new ZetaResponse<>(MetaTableResponse.SUCCESS, HttpStatus.OK);
  }

  public enum ColumnType {
    STRING, VARCHAR, INT, BIGINT, DOUBLE, DECIMAL, DATE, TIMESTAMP
  }


  public Object getZetaMetaTableWhiteList() {
    List<Map<String, Object>> res = jdbcTemplate.queryForList(GET_WHITELIST_QUERY);
    return res.stream().map(m -> m.get("nt")).collect(Collectors.toList());
  }

  public ZetaResponse applyAccess(String nt, String id) {
    LOGGER.info("{} apply access to meta table {}", nt, id);
    ZetaMetaTable zetaMetaTable = findMetaTableById(id);
    Map<AuthType, Set<AuthorizationInfo>> originAuthInfo =
        StringUtils.isBlank(zetaMetaTable.getAuthInfo()) ?
            metaTableAuthorization.initAuthInfo() :
            metaTableAuthorization.parseAuthInfo(zetaMetaTable.getAuthInfo());
    AuthorizationMail authorizationMail = new AuthorizationMail();
    authorizationMail.setOwner(zetaMetaTable.getNt());
    authorizationMail.setJobName(zetaMetaTable.getMetaTableName());
    authorizationMail.setAuthorizationType(ZETA_SHEET);
    authorizationMail.setOriginAuthInfo(originAuthInfo);
    authorizationMail.setLink(META_TABLE_URL);
    metaTableAuthorization.sendRequestEmail(nt, authorizationMail);
    return new ZetaResponse<>(MetaTableResponse.SUCCESS, HttpStatus.OK);

  }
}
