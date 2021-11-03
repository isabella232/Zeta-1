package com.ebay.dss.zds.service.metatable;

import com.ebay.dss.zds.common.JsonUtil;
import com.ebay.dss.zds.common.PropertiesUtil;
import com.ebay.dss.zds.exception.MetaTableException;
import com.ebay.dss.zds.model.JobResult;
import com.ebay.dss.zds.model.Platform;
import com.ebay.dss.zds.model.metatable.MetaTableOperation.TableSchemaInfo;
import com.ebay.dss.zds.model.metatable.ZetaMetaTable;
import com.ebay.dss.zds.service.datamove.HadoopTableOperation;
import com.ebay.dss.zds.service.datamove.TableMetaService;
import com.ebay.dss.zds.service.filehandler.CSVFileHandler;
import com.ebay.dss.zds.service.filehandler.FileHandler;
import com.ebay.dss.zds.service.filestorage.FileType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.ebay.dss.zds.common.PropertiesUtil.ScheduleSwitchKey.MetaTableProd;


@Service("Hadoop")
public class HadoopMetaTableReceiver implements OperationReceiver {

  private static final Logger LOGGER = LoggerFactory.getLogger(HadoopMetaTableReceiver.class);

  @Autowired
  private HadoopTableOperation hadoopTableOperation;

  @Autowired
  private TableMetaService tableMetaService;

  @Value("${zeta-sheet.upload.path}")
  private String UPLOAD_PATH;

  private static final String DROP_SQL = "DROP TABLE IF EXISTS %s;\n";
  private static final String TRUNCATE_SQL = "TRUNCATE TABLE %s;\n";
  private static final String RENAME_SQL = "ALTER TABLE %s RENAME TO %s;\n";

  @Override
  public void create(ZetaMetaTable zetaMetaTable) {
    String table = getHadoopTableName(zetaMetaTable);
    Platform platform = Platform.valueOf(zetaMetaTable.getPlatform());
    String account = getHadoopAccount(zetaMetaTable);
    if (hadoopTableOperation.checkTableExist(
        platform, table, account, zetaMetaTable.getId()).getStatus()) {
      throw new MetaTableException(
          String.format("%s is already exist on %s. Please drop it firstly."
              , table, platform.name()));
    }
    JobResult jobResult = createTable(zetaMetaTable, table);
    if (!jobResult.getStatus()) {
      hadoopTableOperation.closeInterpreterSession(account, zetaMetaTable.getId());
      throw new MetaTableException(jobResult.getOutput());
    }
  }

  private JobResult createTable(ZetaMetaTable zetaMetaTable, String table) {
    List<TableSchemaInfo> tableSchemaInfoList = JsonUtil.fromJson(zetaMetaTable.getSchemaInfo()
        , new TypeReference<List<TableSchemaInfo>>() {
        });
    Platform platform = Platform.valueOf(zetaMetaTable.getPlatform());
    String account = getHadoopAccount(zetaMetaTable);
    Map schema = convertMetaTableSchemaToSpark(tableSchemaInfoList);
    String ddl;
    JobResult jobResult = new JobResult(true);
    switch (zetaMetaTable.getMetaTableType()) {
      case PROD:
        if (checkProdMetaTableUploadSwitch()) {
          ddl = tableMetaService.generateSparkTableDDL(schema,
              "target_table_parquet_standard", table,
              getProdTableLocation(zetaMetaTable));
          LOGGER.info("Execute [CREATE] statement on [{}]: {}", platform, ddl);
          jobResult = hadoopTableOperation.executeSparkSQL(account, platform
              , hadoopTableOperation.getUserQueue(account, platform.getId()), ddl);
        }
    }
    return jobResult;
  }

  @Override
  public void drop(ZetaMetaTable zetaMetaTable) {
    String table = getHadoopTableName(zetaMetaTable);
    Platform platform = Platform.valueOf(zetaMetaTable.getPlatform());
    String account = getHadoopAccount(zetaMetaTable);
    String sql = String.format(DROP_SQL, table);
    switch (zetaMetaTable.getMetaTableType()) {
      case PROD:
        if (checkProdMetaTableUploadSwitch()) {
          LOGGER.info("Execute [DROP] statement on [{}]: {}", platform, sql);
          hadoopTableOperation.executeSparkSQL(account, platform
              , hadoopTableOperation.getUserQueue(account, platform.getId())
              , sql
          );
        }
    }
  }

  @Override
  public Object query(ZetaMetaTable zetaMetaTable) {
    return null;
  }

  @Override
  public void update(ZetaMetaTable zetaMetaTable, Object data) {
    String table = getHadoopTableName(zetaMetaTable);
    Platform platform = Platform.valueOf(zetaMetaTable.getPlatform().trim());
    String account = getHadoopAccount(zetaMetaTable);
    String fileName = getCsvFileName(zetaMetaTable);
    String uploadPath = UPLOAD_PATH + "metatable/";
    // generate csv file
    String localFullPath = String.format("%s%s", uploadPath, fileName);
    FileHandler fileHandler = new FileHandler(new CSVFileHandler());
    File tempFile = fileHandler.executeWriteToFile(localFullPath, data, null);
    // check schema updated
    boolean schemaUpdateFlag = isSchemaUpdated(zetaMetaTable);
    LOGGER.info("Zeta Sheet [{}] schemaUpdateFlag: {}"
        , zetaMetaTable.getMetaTableName(), schemaUpdateFlag);
    String tempTable = table + "_" + (int) (Math.random() * 1000);
    LOGGER.info("Temp Table name is {} for Table {} on {}", tempTable, table, platform);
    // create table if not exist
    JobResult jobResult = schemaUpdateFlag ? createTable(zetaMetaTable, tempTable)
        : createTableIfNotExist(zetaMetaTable, platform, account, table);
    // load data
    jobResult = jobResult.getStatus() ?
        loadDataToTable(zetaMetaTable, account, schemaUpdateFlag ? tempTable : table
            , localFullPath, fileName, platform, tempFile) : jobResult;
    // convert temp table to prod if needed
    jobResult = schemaUpdateFlag && jobResult.getStatus() ?
        convertTempTableToProd(zetaMetaTable, tempTable) : jobResult;
    hadoopTableOperation.closeInterpreterSession(account, zetaMetaTable.getId());
    if (!jobResult.getStatus()) {
      throw new MetaTableException(jobResult.getOutput());
    }
  }

  private JobResult createTableIfNotExist(ZetaMetaTable zetaMetaTable
      , Platform platform, String account, String table) {
    if (!hadoopTableOperation.checkTableExist(
        platform, table, account, zetaMetaTable.getId()).getStatus()) {
      // create table if not exist
      return createTable(zetaMetaTable, table);
    }
    return new JobResult(true);
  }

  private JobResult loadDataToTable(ZetaMetaTable zetaMetaTable, String account
      , String table, String localFullPath, String fileName, Platform platform, File tempFile) {
    switch (zetaMetaTable.getMetaTableType()) {
      case PROD:
        if (checkProdMetaTableUploadSwitch()) {
          if (Objects.isNull(tempFile)) {
            return new JobResult(true);
          }
          // upload to hadoop
          String tempHdfsPath = String.format("/user/%s/", account);
          LOGGER.info("Execute [UPLOAD] File [{}] to {} HDFS", tempHdfsPath, platform);
          JobResult jobResult = hadoopTableOperation.uploadFileToHdfs(
              platform, account, localFullPath, tempHdfsPath, fileName);
          LOGGER.info("Execute [INSERT OVERWRITE] [{}] on {}", table, platform);
          return jobResult.getStatus() ? hadoopTableOperation.executeSparkSQL(account, platform
              , hadoopTableOperation.getUserQueue(account, platform.getId())
              , genInsertSparkSQL(table, tempHdfsPath + fileName)) : jobResult;
        }
    }
    return new JobResult(true);
  }

  private String genInsertSparkSQL(String table, String hdfsFullPath) {
    StringBuilder queries = new StringBuilder();
    String viewName = table.replace(".", "_") + (int) (Math.random() * 1000);
    queries.append(tableMetaService.genTempView(viewName, hdfsFullPath,
        FileType.CSV, false));
    queries.append("\n").append(tableMetaService.genInsertQuery(table, viewName));
    LOGGER.info("Generate [INSERT OVERWRITE] statement: [{}]", queries);
    return queries.toString();
  }

  private boolean isSchemaUpdated(ZetaMetaTable zetaMetaTable) {
    if (Objects.nonNull(zetaMetaTable.getHadoopSchemaInfo())) {
      List<TableSchemaInfo> tableSchema = JsonUtil.fromJson(zetaMetaTable.getSchemaInfo()
          , new TypeReference<List<TableSchemaInfo>>() {
          });
      List<TableSchemaInfo> hadoopSchema = JsonUtil.fromJson(zetaMetaTable.getHadoopSchemaInfo()
          , new TypeReference<List<TableSchemaInfo>>() {
          });
      return !(tableSchema.size() == hadoopSchema.size() && compareSchema(tableSchema, hadoopSchema));
    }
    return false;
  }

  private boolean compareSchema(List<TableSchemaInfo> tableSchema
      , List<TableSchemaInfo> hadoopSchema) {
    for (TableSchemaInfo tableSchemaInfo : tableSchema) {
      boolean flag = hadoopSchema.stream().anyMatch(s ->
          s.getColumn().equalsIgnoreCase(tableSchemaInfo.getColumn())
              && s.getType().equalsIgnoreCase(tableSchemaInfo.getType()));
      if (!flag) {
        return false;
      }
    }
    return true;
  }

  private JobResult convertTempTableToProd(ZetaMetaTable zetaMetaTable, String tempTable) {
    Platform platform = Platform.valueOf(zetaMetaTable.getPlatform());
    String account = getHadoopAccount(zetaMetaTable);
    String table = getHadoopTableName(zetaMetaTable);
    JobResult jobResult = new JobResult(true);
    String convertSQL = String.format(DROP_SQL, table)
        + String.format(RENAME_SQL, tempTable, table);
    switch (zetaMetaTable.getMetaTableType()) {
      case PROD:
        if (checkProdMetaTableUploadSwitch()) {
          LOGGER.info("Execute [DROP+RENAME] statement on [{}]: {}", platform, convertSQL);
          jobResult = hadoopTableOperation.executeSparkSQL(account, platform
              , hadoopTableOperation.getUserQueue(account, platform.getId())
              , convertSQL);
        }
    }
    return jobResult;
  }

  private Map<String, String> convertMetaTableSchemaToSpark(List<TableSchemaInfo> tableSchemaInfo) {
    Map<String, String> schema = Maps.newLinkedHashMap();
    for (TableSchemaInfo info : tableSchemaInfo) {
      String dataType = info.getType();
      switch (ZetaMetaTableService.ColumnType.valueOf(dataType.toUpperCase().trim())) {
        case VARCHAR:
        case DECIMAL:
          String length = info.getLength();
          dataType = StringUtils.isBlank(length) || "0".equals(length) ?
              dataType : String.format("%s (%s)", dataType, length);
          break;

      }
      schema.put(info.getColumn(), dataType);
    }
    return schema;
  }

  private String getHadoopTableName(ZetaMetaTable zetaMetaTable) {
    return String.format("%s.%s", zetaMetaTable.getDb(), zetaMetaTable.getTbl());
  }

  private String getProdTableLocation(ZetaMetaTable zetaMetaTable) {
    return String.format("/sys/edw/working/prs_dw/prs_w/mrktng_hlth/%s/"
        , zetaMetaTable.getTbl());
  }

  @Deprecated
  private String getParquetFileName(ZetaMetaTable zetaMetaTable) {
    return String.format("%s_%s_%s.parquet"
        , zetaMetaTable.getDb(), zetaMetaTable.getTbl(), zetaMetaTable.getPlatform());
  }

  private String getCsvFileName(ZetaMetaTable zetaMetaTable) {
    return String.format("%s_%s_%s.csv"
        , zetaMetaTable.getDb(), zetaMetaTable.getTbl(), zetaMetaTable.getPlatform());
  }

  private String getHadoopAccount(ZetaMetaTable zetaMetaTable) {
    return Objects.isNull(zetaMetaTable.getAccount()) ?
        zetaMetaTable.getNt() : zetaMetaTable.getAccount();
  }

  private boolean checkProdMetaTableUploadSwitch() {
    return PropertiesUtil.isScheduleSwitchOn(MetaTableProd);
  }
}
