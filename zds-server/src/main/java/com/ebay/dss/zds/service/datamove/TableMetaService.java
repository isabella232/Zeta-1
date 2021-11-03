package com.ebay.dss.zds.service.datamove;

import com.ebay.dss.zds.common.JsonUtil;
import com.ebay.dss.zds.common.PropertiesUtil;
import com.ebay.dss.zds.model.DataMoveDetail;
import com.ebay.dss.zds.model.Platform;
import com.ebay.dss.zds.service.filestorage.FileType;
import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import com.jayway.jsonpath.PathNotFoundException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.ebay.dss.zds.exception.ToolSetCheckException;
import com.ebay.dss.zds.model.Column;
import com.ebay.dss.zds.model.Table;
import com.jayway.jsonpath.JsonPath;

import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.Resource;


@Component
public class TableMetaService {
  private static final Logger LOGGER = LoggerFactory.getLogger(TableMetaService.class);

  @Resource(name = "resttemplate")
  private RestTemplate restTemplate;

  private static final String TABLE_NAME = "${tableName}";
  private static final String LOCATION = "${location}";
  private static final String COLUMNS = "${columnList}";
  //  private static final String DOE_GET_COLUMNS = "https://doe.corp.ebay.com/api/asset/getTableColumnInfo";
  //  private static final String DOE_GET_COLUMNS_ALL = "http://doe.corp.ebay.com/api/asset/ddsGetColumn?platform=%s&db=%s&table=%s";
  private static String DOE_GET_COLUMNS;
  private static String DOE_GET_COLUMNS_ALL;

  @Value("${doe.old.url}")
  private void initServiceUrl(String hostUrl) {
    DOE_GET_COLUMNS = hostUrl + "/asset/getTableColumnInfo";
    DOE_GET_COLUMNS_ALL = hostUrl + "/asset/ddsGetColumn?platform=%s&db=%s&table=%s";
  }

  public String generateSparkTableDDL(Object schema, String templateName,
                                      String spTblName, String location) {
    LOGGER.info("Start generate DDL for table {}", spTblName);

    StringBuilder ddl = new StringBuilder();
    String template = PropertiesUtil.getTemplate(templateName);
    ddl.append(template
        .replace(TABLE_NAME, spTblName)
        .replace(COLUMNS, getSparkColumnList(schema))
        .replace(LOCATION, location));
    LOGGER.debug(ddl.toString());
    return ddl.toString();
  }

  public String generateHermesTableDDL(Object schema, String spTblName) {
    LOGGER.info("Start generate DDL for hermes table {}", spTblName);

    StringBuilder ddl = new StringBuilder();
    String template = PropertiesUtil.getTemplate("target_table_parquet_hermes");
    ddl.append(template
        .replace(TABLE_NAME, spTblName)
        .replace(COLUMNS, getSparkColumnList(schema)));
    LOGGER.debug(ddl.toString());
    return ddl.toString();
  }

  public String getSparkColumnList(Object schema) {
    String columnList;
    if (schema instanceof Map) {
      Set<String> columns = ((Map) schema).keySet();
      columnList = columns.stream()
          .map(column -> String.format("`%s` %s",
              column, ((Map) schema).get(column)))
          .collect(Collectors.joining("\n,"));
    } else if (schema instanceof Table) {
      List<Column> columns = sortTableColumns((Table) schema);
      columnList = columns.stream()
          .map(column -> String.format("`%s` %s",
              column.getColumnname().toLowerCase(),
              PropertiesUtil.getSparkType(column)))
          .collect(Collectors.joining("\n,"));
    } else {
      throw new ToolSetCheckException("Only support Map and Table schema");
    }
    return columnList;
  }

  public List<Column> sortTableColumns(Table table) {
    return table.getColumns().stream()
        .sorted(Comparator.comparing(Column::getColumnid))
        .collect(Collectors.toList());
  }

  public String generateAvroTableDDL(String spTblName, String location) {
    LOGGER.info("Start generate Spark SQL for table {}", spTblName);
    StringBuilder ddl = new StringBuilder();
    String template = PropertiesUtil.getTemplate("target_table_avro_spark");
    ddl.append(template
        .replace(TABLE_NAME, spTblName)
        .replace(LOCATION, location));
    LOGGER.debug(ddl.toString());
    return ddl.toString();
  }

  public String generateTDBridgeSQL(Table table, String filter) {
    LOGGER.info("Start generate data move query for table {}", table.getTableName());
    StringBuilder tdQuery = new StringBuilder("select ");
    List<Column> columns = sortTableColumns(table);

    tdQuery.append(columns.stream().map(column ->
        convertTdBridgeColumn(column)).collect(Collectors.joining(",")));
    tdQuery.append(" from " + table.getDatabaseName() + "." + table.getTableName());
    if (filter != null && filter.length() > 0) {
      tdQuery.append(" where " + filter.replace("where", ""));
    }
    LOGGER.info("Td Bridge Query: {}", tdQuery.toString());
    if (tdQuery.toString().contains("base64m")) {
      return "select * from (" + tdQuery.toString() + ") a";
    }
    return tdQuery.toString();
  }


  private String convertTdBridgeColumn(Column column) {
    StringBuilder columnT = new StringBuilder();
    String columnType = column.getColumntype().trim().toLowerCase();
    switch (columnType) {
      case "timestamp":
        columnT.append("cast(")
            .append(column.getColumnname())
            .append(" as varchar(23)) as ")
            .append(column.getColumnname());
        break;
      case "date":
        columnT.append("cast(cast(")
            .append(column.getColumnname())
            .append(" as format 'YYYY-MM-DD') as varchar(10)) as ")
            .append(column.getColumnname());
        break;
      case "time":
        columnT.append("cast(")
            .append(column.getColumnname())
            .append(" as varchar(10)) as ")
            .append(column.getColumnname());
        break;
      case "byte":
        columnT.append("cast(from_bytes(")
            .append(column.getColumnname())
            .append(",'base64m') as varchar(256)) as ")
            .append(column.getColumnname());
        break;
      case "char":
        columnT.append("cast(")
            .append(column.getColumnname())
            .append(" as varchar(")
            .append(column.getColumnlength())
            .append(")) as ")
            .append(column.getColumnname());
        break;
      case "number":
        int scale = column.getDecimalfractionaldigits() < 0 ? 38 : column.getDecimaltotaldigits();
        int precious = column.getDecimaltotaldigits() < 0 ? 2 : column.getDecimalfractionaldigits();
        columnT.append("cast(")
            .append(column.getColumnname())
            .append(" as decimal(")
            .append(scale)
            .append(",")
            .append(precious)
            .append(")) as ")
            .append(column.getColumnname());
        break;
      default:
        columnT.append(column.getColumnname());
        break;
    }
    return columnT.toString();
  }

  public String genTempView(String spViewName, String location,
                            FileType fileType, boolean isGlobal) {
    StringBuilder content = new StringBuilder();
    String type = isGlobal ? "CREATE OR REPLACE GLOBAL TEMP VIEW " :
        "CREATE OR REPLACE TEMPORARY VIEW ";
    switch (fileType) {
      case CSV:
        content.append(type).append(spViewName).append("\n")
            .append("USING com.databricks.spark.csv").append("\n")
            .append("OPTIONS (path \"").append(location)
            .append("\", header \"true\", quote \"\\\"\", escape \"\\\"\", multiLine \"true\",inferSchema \"true\");\n");
        break;

      case AVRO:
        content.append(type).append(spViewName).append("\n")
            .append("USING com.databricks.spark.avro").append("\n")
            .append("OPTIONS (path \"").append(location).append("avro\");\n");
        break;

      case PARQUET:
        content.append(type).append(spViewName).append("\n")
            .append("USING parquet").append("\n")
            .append("OPTIONS (path \"").append(location).append("\");\n");
        break;

      default:
        break;
    }
    LOGGER.debug("Temp View: {}", content);
    return content.toString();
  }

  public String genInsertQuery(String spTblName, String tempView, Object... schema) {
    StringBuilder content = new StringBuilder();
    String columns = "*";
    if (ArrayUtils.isNotEmpty(schema)) {
      List<Column> columnList = sortTableColumns((Table) schema[0]);
      columns = columnList.stream().map(column -> convertInsertColumn(column))
          .collect(Collectors.joining("\n,"));
    }
    content.append("insert overwrite table ")
        .append(spTblName).append("\n")
        .append("select ").append("\n")
        .append(columns).append("\n")
        .append("from ").append(tempView).append(";");

    LOGGER.debug(content.toString());
    return content.toString();
  }


  private String convertInsertColumn(Column column) {
    StringBuilder columnT = new StringBuilder();
    if (column.getDataType().toLowerCase().startsWith("date")) {
      columnT.append("regexp_replace(").append(column.getColumnname()).append(",\"/\",\"-\")");
    } else {
      columnT.append(column.getColumnname());
    }
    return columnT.toString();
  }

  public String getTempViewName(DataMoveDetail dataMoveDetail) {
    return String.format("%s_temp_view_%s",
        dataMoveDetail.getHistory().getTargetTable().replace(".", "_")
            .replace(" ", ""),
        dataMoveDetail.getHistory().getHistoryId());
  }

  @Deprecated
  public String getTempTableName(DataMoveDetail dataMoveDetail) {
    return String.format("%s_temp_tbl_%s",
        dataMoveDetail.getHistory().getTargetTable().replace(" ", ""),
        dataMoveDetail.getHistory().getHistoryId());
  }

  public Table getTableSchema(String platform, String dbName, String tblName, MetadataType metadataType) {
    LOGGER.info("Get {} Info", tblName);
    Table table = new Table(tblName, dbName, platform);
    List<Map<String, Object>> columnList;
    switch (metadataType) {
      case REALTIME:
        columnList = getRealtimeColumnInfo(platform, dbName, tblName);
        break;
      case DAILY:
        columnList = getDailyColumnInfo(platform, dbName, tblName);
        if (CollectionUtils.isEmpty(columnList)) {
          columnList = getRealtimeColumnInfo(platform, dbName, tblName);
        }
        break;
      default:
        throw new ToolSetCheckException(String.format("Metadata Not support %s type", metadataType));
    }

    if (Objects.isNull(columnList)) {
      throw new ToolSetCheckException("Metadata API not work, Please try later");
    }

    if (columnList.isEmpty()) {
      throw new ToolSetCheckException("Table Not Exist");
    }
    return setTableColumnDataType(table, columnList);

  }


  private Table setTableColumnDataType(Table table, List<Map<String, Object>> columnList) {
    Set<Column> columns = new LinkedHashSet<>();
    for (Map<String, Object> obj : columnList) {
      Column column = (Column) JsonUtil.mapToObject(obj, Column.class);
      if (column.getColumnname() == null || column.getColumnname().length() == 0) {
        throw new ToolSetCheckException("Table column have null value");
      }
      String columnType = column.getColumntype();
      switch (columnType) {
        case "DECIMAL":
          column.setDataType(
              "DECIMAL(" + column.getDecimaltotaldigits() + "," + column.getDecimalfractionaldigits() + ")");
          break;
        case "CHAR":
          if (column.getColumnlength() == 0) {
            column.setColumnlength(200);
          }
          column.setDataType("CHAR(" + column.getColumnlength() + ")");
          break;
        case "VARCHAR":
          column.setDataType("VARCHAR(" + column.getColumnlength() + ")");
          break;
        default:
          column.setDataType(columnType);
          break;
      }
      columns.add(column);
    }
    table.setColumns(columns);
    return table;
  }

  private List<Map<String, Object>> getDailyColumnInfo(String platform, String dbName, String tblName) {
    // check daily API
    ResponseEntity<String> responseEntity = restTemplate.getForEntity(String.format(DOE_GET_COLUMNS_ALL, platform, dbName, tblName),
        String.class);
    if (responseEntity.getStatusCode() == HttpStatus.OK) {
      String columnStr = responseEntity.getBody()
          .replace("column_name", "columnname")
          .replace("data_type_desc", "columntype")
          .replace("decimal_total_digits", "decimaltotaldigits")
          .replace("decimal_fractional_digits", "decimalfractionaldigits")
          .replace("data_length_cnt", "columnlength")
          .replace("column_id", "columnid");
      try {
        return JsonPath.parse(columnStr).read("$.data.value");
      } catch (PathNotFoundException e) {
        LOGGER.error("Parse GET COLUMN ERROR.", e);
      }
    }
    return null;
  }

  private List<Map<String, Object>> getRealtimeColumnInfo(String platform, String dbName, String tblName) {
    // check Realtime API
    Map<String, String> request = Maps.newLinkedHashMap();
    request.put("platform", platform);
    request.put("table", tblName);
    request.put("db", dbName);
    ResponseEntity<String> responseEntity = restTemplate.postForEntity(DOE_GET_COLUMNS, request, String.class);
    if (responseEntity.getStatusCode() == HttpStatus.OK) {
      try {
        return JsonPath.parse(responseEntity.getBody()).read("$.data.value");
      } catch (PathNotFoundException e) {
        LOGGER.error("Parse GET COLUMN ERROR.", e);
      }
    }
    return null;
  }

  public String getDBName(String tableName) {
    List<String> tableInfo = splitTableName(tableName);
    return tableInfo.size() > 0 ? tableInfo.get(0) : null;
  }

  public String getTableName(String tableName) {
    List<String> tableInfo = splitTableName(tableName);
    return tableInfo.size() > 1 ? tableInfo.get(1) : null;
  }

  private List<String> splitTableName(String tableName) {
    return Splitter.on(".").trimResults().omitEmptyStrings().splitToList(tableName);
  }

  public enum MetadataType {
    REALTIME, DAILY
  }

  public String getTDBridgeAvroUploadPath(DataMoveDetail dataMoveDetail) {
    return String.format("/sys/edw/zeta_dev/datamove/%s/%s/",
        dataMoveDetail.getHistory().getNt().trim(),
        dataMoveDetail.getHistory().getSourceTable().trim().toLowerCase());
  }

  public String getTDBridgeAvroFullPath(DataMoveDetail dataMoveDetail) {
    return dataMoveDetail.getTdBridgeAvro() + "avro/";
  }

  public String getTD2HDTargetLocation(DataMoveDetail dataMoveDetail) {
    return String.format("/sys/edw/zeta_dev/datamove/%s/",
        dataMoveDetail.getHistory().getTargetTable().trim());
  }

  public String getFileUploadHDFSPath(DataMoveDetail dataMoveDetail) {
    StringBuilder content = new StringBuilder();
    if (FileType.getFileTypeByPath(dataMoveDetail.getHistory().getSourceTable()).equals(FileType.PARQUET)) {
      content.append(getLC2HDTargetLocation(dataMoveDetail));
    } else {
      if (Platform.valueOf(dataMoveDetail.getHistory().getTargetPlatform()).getId() == Platform.hermes.getId()) {
        content.append("viewfs://apollo-rno");
      }
      content.append(String.format("/user/%s/upload/",
          dataMoveDetail.getHistory().getNt()));
    }

    return content.toString();
  }

  public String getFileUploadHDFSFullPath(DataMoveDetail dataMoveDetail) {
    return getFileUploadHDFSPath(dataMoveDetail) + dataMoveDetail.getHistory().getSourceTable();
  }


  public String getLC2HDTargetLocation(DataMoveDetail dataMoveDetail) {
    StringBuilder content = new StringBuilder();
    if (Platform.valueOf(dataMoveDetail.getHistory().getTargetPlatform()).getId() == Platform.hermes.getId()) {
      content.append("viewfs://apollo-rno");
    }
    content.append(String.format("/user/%s/%s/",
        dataMoveDetail.getHistory().getNt(),
        dataMoveDetail.getHistory().getTargetTable().trim()));

    return content.toString();
  }

}
