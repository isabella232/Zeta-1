package com.ebay.dss.zds.service.metatable;

import com.ebay.dss.zds.common.JsonUtil;
import com.ebay.dss.zds.common.PropertiesUtil;
import com.ebay.dss.zds.exception.MetaTableException;
import com.ebay.dss.zds.model.metatable.MetaTableOperation;
import com.ebay.dss.zds.model.metatable.MetaTableOperation.Operations;
import com.ebay.dss.zds.model.metatable.MetaTableOperation.TableSchemaInfo;
import com.ebay.dss.zds.model.metatable.ZetaMetaTable;

import com.ebay.dss.zds.service.filehandler.CSVFileHandler;
import com.ebay.dss.zds.service.filehandler.FileHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.ebay.dss.zds.service.metatable.ZetaMetaTableService.ColumnType;

import static com.ebay.dss.zds.common.JsonUtil.MAPPER;
import static com.ebay.dss.zds.model.metatable.MetaTableOperation.Operations.*;
import static com.ebay.dss.zds.model.metatable.MetaTableOperation.Operations.INDEXADD;
import static com.ebay.dss.zds.service.metatable.OLAPMetaTableReceiver.TableField._ID_;
import static com.ebay.dss.zds.service.metatable.OLAPMetaTableReceiver.TableField._VERSION_;

@Service("OLAP")
public class OLAPMetaTableReceiver implements OperationReceiver {

  private static final Logger LOGGER = LoggerFactory.getLogger(OLAPMetaTableReceiver.class);

  private static final String TABLE_NAME = "${tableName}";
  private static final String COLUMNS = "${columnList}";
  private static final String UNIQUE_KEY = "${uniqueKey}";

  private static final String UNIQUE_KEY_NAME = "UNIQUE_NAME";
  private static final String CURRENT_TIMESTAMP = "CURRENT_TIMESTAMP()";
  // table schema
  private static final String RENAME_TABLE = "ALTER TABLE %s RENAME TO %s;\n";
  private static final String ADD_COLUMN = "ALTER TABLE %s ADD %s;\n";
  private static final String DROP_COLUMN = "ALTER TABLE %s DROP %s;\n";
  private static final String ADD_INDEX = "ALTER TABLE %s ADD %s;\n";
  private static final String DROP_INDEX = "ALTER TABLE %s DROP INDEX %s;\n";
  private static final String SELECT_INDEX = "SELECT * FROM information_schema.statistics WHERE table_schema='%s' AND table_name = '%s' AND index_name = '%s'";

  // table
  private static final String SELECT_TABLE = "SELECT _ID_,%s,CAST(_VERSION_ as char) AS _VERSION_ FROM %s;\n";
  private static final String UPDATE_TABLE = "UPDATE %s SET %s,%s = %s WHERE %s = %s AND %s = '%s';\n";
  private static final String INSERT_TABLE = "INSERT INTO %s (%s) VALUES %s;\n";
  private static final String DELETE_TABLE = "DELETE FROM %s WHERE %s = %s AND %s = '%s';\n";

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Value("${zete-sheet.db}")
  private String DB_NAME;

  @Value("${zeta-sheet.upload.path}")
  private String UPLOAD_PATH;

  @Override
  public void create(ZetaMetaTable zetaMetaTable) {
    List<TableSchemaInfo> tableSchemaInfoList = JsonUtil.fromJson(zetaMetaTable.getSchemaInfo()
        , new TypeReference<List<TableSchemaInfo>>() {
        });
    String query = generateQuery(CREATE, getOlapTableName(zetaMetaTable), tableSchemaInfoList);
    for (String sql : Splitter.on(";\n").omitEmptyStrings().splitToList(query)) {
      LOGGER.info("Execute [{}] statement: {}", CREATE, sql);
      jdbcTemplate.execute(sql);
    }
  }

  @Override
  public void drop(ZetaMetaTable zetaMetaTable) {
    String query = Optional.ofNullable(generateQuery(REMOVE, getOlapTableName(zetaMetaTable), null))
        .orElseThrow(() -> new MetaTableException("DROP Query Get failed"));
    LOGGER.info("Execute [{}] statement: {}", REMOVE, query);
    jdbcTemplate.execute(query);
  }

  @Override
  public Object query(ZetaMetaTable zetaMetaTable) {
    List<TableSchemaInfo> schemaList = JsonUtil.fromJson(zetaMetaTable.getSchemaInfo()
        , new TypeReference<List<TableSchemaInfo>>() {
        });
    String query = Optional.ofNullable(generateQuery(SELECT, getOlapTableName(zetaMetaTable), schemaList))
        .orElseThrow(() -> new MetaTableException("SELECT Query Get failed"));
    LOGGER.info("Execute [{}] statement: {}", SELECT, query);
    return jdbcTemplate.queryForList(query);
  }

  @Override
  @Transactional(rollbackFor = {Exception.class, RuntimeException.class})
  public void update(ZetaMetaTable zetaMetaTable, Object data) {
    if (data instanceof MetaTableOperation) {
      MetaTableOperation metaTableOperation = (MetaTableOperation) data;
      Map<Operations, List<Object>> operations = metaTableOperation.getOperations();
      Object snapshotData = query(zetaMetaTable);
      if (!CollectionUtils.isEmpty(operations)) {
        for (Map.Entry<Operations, List<Object>> entry : operations.entrySet()) {
          Operations op = entry.getKey();
          List<Object> value = entry.getValue();
          switch (op) {
            case ADD:
            case DROP:
              for (Object obj : value) {
                LOGGER.info("Execute [{}], info [{}]", op, obj);
                TableSchemaInfo schemaInfo = MAPPER.convertValue(obj, TableSchemaInfo.class);
                executeQuery(zetaMetaTable, schemaInfo, op);
                updateSchema(zetaMetaTable, schemaInfo, op);
              }
              break;
            case INSERT:
              LOGGER.info("Execute [{}], info [{}]", op, value);
              executeQuery(zetaMetaTable, handleNumberValue(OperationReceiver.getSchemaMap(zetaMetaTable), value), op);
              break;
            case DELETE:
            case UPDATE:
              for (Object obj : handleNumberValue(OperationReceiver.getSchemaMap(zetaMetaTable), value)) {
                LOGGER.info("Execute [{}], info [{}]", op, obj);
                executeQuery(zetaMetaTable, obj, op);
              }
              break;
            default:
              throw new MetaTableException("Update Table Not Support Operation - " + op);
          }
        }
      }
      generateSnapShot(zetaMetaTable, snapshotData);
    }
  }

  private void generateSnapShot(ZetaMetaTable zetaMetaTable, Object data) {
    String fileName = String.format("%s_%s_%s.csv", zetaMetaTable.getId()
        , System.currentTimeMillis(), zetaMetaTable.getLastModifier());
    String uploadPath = UPLOAD_PATH + "metatable_snapshot/";
    // generate csv file
    String localFullPath = String.format("%s%s", uploadPath, fileName);
    LOGGER.info("Start to save sheet [{}] snapshot file: {}"
        , zetaMetaTable.getMetaTableName(), localFullPath);
    FileHandler fileHandler = new FileHandler(new CSVFileHandler());
    fileHandler.executeWriteToFile(localFullPath, data, null);
  }

  private List handleNumberValue(Map<String, String> schema, List data) {
    if (Objects.nonNull(data)) {
      List<Map<String, String>> dataList = (List<Map<String, String>>) data;
      for (Map<String, String> dataMap : dataList) {
        for (Map.Entry<String, String> map : dataMap.entrySet()) {
          String column = map.getKey();
          if (StringUtils.isNotBlank(map.getValue())
              && schema.containsKey(column)
              && isNumberType(schema.get(column))) {
            map.setValue(map.getValue().replace(",", ""));
          }
        }
      }
      return dataList;
    }
    return data;
  }

  private boolean isNumberType(String type) {
    switch (ColumnType.valueOf(type.trim().toUpperCase())) {
      case INT:
      case BIGINT:
      case DOUBLE:
      case DECIMAL:
        return true;
      default:
        return false;
    }
  }

  @Transactional(rollbackFor = {Exception.class, RuntimeException.class})
  public void executeQuery(ZetaMetaTable zetaMetaTable, Object info, Operations op) {
    String query = generateQuery(op, getOlapTableName(zetaMetaTable), info);
    if (Objects.nonNull(query)) {
      LOGGER.info("Execute [{}] statement: {}", op, query);
      switch (op) {
        case ADD:
        case DROP:
        case INDEXADD:
        case INDEXDROP:
          jdbcTemplate.execute(query);
          break;
        case INSERT:
        case UPDATE:
        case DELETE:
          if (jdbcTemplate.update(query) == 0) {
            throw new RuntimeException("The data is not stale. Please refresh and have a retry.");
          }
          break;

        default:
          throw new MetaTableException("Not Support Operation - " + op);
      }
    }
  }

  private void updateSchema(ZetaMetaTable zetaMetaTable, TableSchemaInfo schemaInfo, Operations op) {
    List<TableSchemaInfo> schemaList = JsonUtil.fromJson(zetaMetaTable.getSchemaInfo()
        , new TypeReference<List<TableSchemaInfo>>() {
        });
    switch (op) {
      case ADD:
        schemaList.add(schemaInfo);
        break;
      case DROP:
        schemaList.removeIf(obj -> obj.getColumn().equalsIgnoreCase(schemaInfo.getColumn()));
        break;
    }
    // add index
    if (hasUniqueKey(schemaList)) {
      // drop index if exist
      if (isUniqueKeyExist(zetaMetaTable)) {
        executeQuery(zetaMetaTable, null, INDEXDROP);
      }
      executeQuery(zetaMetaTable, schemaList, INDEXADD);
    }
    zetaMetaTable.setSchemaInfo(JsonUtil.toJson(schemaList));
  }


  private String getOlapTableName(ZetaMetaTable zetaMetaTable) {
    String tblName = String.format(DB_NAME + ".%s"
        , zetaMetaTable.getId().replace("-", ""));
    LOGGER.info("Get Zeta MetaTableName [{}]", tblName);
    return tblName;
  }


  private String generateQuery(Operations op, String tblName, Object info) {
    switch (op) {
      case CREATE:
        if (info instanceof List) {
          return generateOlapTableDDL(tblName, (List<TableSchemaInfo>) info);
        }
        break;
      case REMOVE:
        return String.format(RENAME_TABLE, tblName, tblName + "_" + System.currentTimeMillis());
      case ADD:
        if (info instanceof TableSchemaInfo) {
          TableSchemaInfo schemaInfo = (TableSchemaInfo) info;
          return String.format(ADD_COLUMN, tblName, convertOlapTableColumn(schemaInfo));
        }
        break;
      case DROP:
        if (info instanceof TableSchemaInfo) {
          TableSchemaInfo schemaInfo = (TableSchemaInfo) info;
          return String.format(DROP_COLUMN, tblName, getColumnName(schemaInfo.getColumn()));
        }
        break;
      case INDEXADD:
        if (info instanceof List) {
          return String.format(ADD_INDEX, tblName, getUniqueKey((List<TableSchemaInfo>) info));
        }
        break;
      case INDEXDROP:
        return String.format(DROP_INDEX, tblName, UNIQUE_KEY_NAME);
      case SELECT:
        if (info instanceof List) {
          String columnInfo = ((List<TableSchemaInfo>) info).stream()
              .map(TableSchemaInfo::getColumn)
              .filter(this::checkColumnValidate)
              .map(this::convertColumn)
              .collect(Collectors.joining(","));
          return String.format(SELECT_TABLE, columnInfo, tblName);
        }
        break;
      case UPDATE:
        if (info instanceof Map) {
          Map<String, Object> tableInfo = (Map<String, Object>) info;
          Object id = tableInfo.remove(_ID_.name());
          Object version = tableInfo.remove(_VERSION_.name());
          String updateInfo = tableInfo.entrySet().stream()
              .map(entry -> String.format("%s = %s"
                  , getColumnName(entry.getKey())
                  , getColumnValue(entry.getValue())))
              .collect(Collectors.joining(","));
          LOGGER.info("UPDATE Value Info: {}", updateInfo);
          return String.format(UPDATE_TABLE, tblName, updateInfo, _VERSION_, CURRENT_TIMESTAMP
              , _ID_, id, _VERSION_, version);
        }
        break;
      case INSERT:
        if (info instanceof List) {
          List<Map<String, Object>> tableInfo = (List<Map<String, Object>>) info;
          if (tableInfo.size() > 0) {
            String columnInfo = "";
            List<String> valueList = Lists.newLinkedList();
            for (Map<String, Object> row : tableInfo) {
              row.remove(_ID_.name());
              row.remove(_VERSION_.name());

              columnInfo = columnInfo.isEmpty() ?
                  row.keySet().stream().map(this::getColumnName)
                      .collect(Collectors.joining(","))
                  : columnInfo;
              valueList.add(String.format("(%s)", row.values().stream()
                  .map(this::getColumnValue).collect(Collectors.joining(","))));
            }
            LOGGER.info("INSERT Column Info: {}", columnInfo);
            LOGGER.info("INSERT Value Info: {}", valueList);
            return String.format(INSERT_TABLE, tblName, columnInfo, Joiner.on(",").join(valueList));
          }
        }
        break;
      case DELETE:
        if (info instanceof Map) {
          return String.format(DELETE_TABLE, tblName
              , _ID_, ((Map) info).get(_ID_.name())
              , _VERSION_, ((Map) info).get(_VERSION_.name()));
        }
        break;
      default:
        throw new MetaTableException("Not Support Operation - " + op);
    }
    return null;
  }

  private String getColumnName(String name) {
    return StringUtils.isNotBlank(name) ? String.format("`%s`", name) : name;
  }

  private String getColumnValue(Object value) {
    return Objects.nonNull(value) && (!value.toString().isEmpty()) ?
        String.format("'%s'", value.toString().replace("'", "''")) : "NULL";
  }

  private String generateOlapTableDDL(String tblName, List<TableSchemaInfo> tableSchemaInfo) {
    String columnList = tableSchemaInfo.stream()
        .filter(info -> checkColumnValidate(info.getColumn()))
        .map(this::convertOlapTableColumn)
        .collect(Collectors.joining(",\n"));
    String uniqueKey = getUniqueKey(tableSchemaInfo);

    return PropertiesUtil.getTemplate("target_table_mysql_standard")
        .replace(TABLE_NAME, tblName)
        .replace(COLUMNS, columnList.isEmpty() ? ""
            : String.format("%s,", columnList))
        .replace(UNIQUE_KEY, uniqueKey.isEmpty() ? ""
            : String.format("%s,", uniqueKey));
  }

  private String convertColumn(String column) {
    String columnName = getColumnName(column);
    /*
    if (schema.getType().equalsIgnoreCase("date")
        || schema.getType().equalsIgnoreCase("timestamp")) {
      return String.format("cast(%s as char) as %s", column, column);
    }
     */
    return String.format("cast(%s as char) as %s", columnName, columnName);
  }

  private boolean checkColumnValidate(String column) {
    return !(_ID_.name().equals(column) || _VERSION_.name().equals(column));
  }

  private String convertOlapTableColumn(TableSchemaInfo info) {
    return String.format("%s %s %s"
        , getColumnName(info.getColumn())
        , getOlapTableColumnDataType(info)
        , info.isNullValue() ? "DEFAULT NULL" : "NOT NULL");
  }

  private boolean isUniqueKeyExist(ZetaMetaTable zetaMetaTable) {
    String query = String.format(SELECT_INDEX
        , DB_NAME
        , getOlapTableName(zetaMetaTable).replace(DB_NAME + ".", "")
        , UNIQUE_KEY_NAME);
    LOGGER.info("Check index exist: {}", query);
    return !jdbcTemplate.queryForList(query).isEmpty();
  }

  private boolean hasUniqueKey(List<TableSchemaInfo> tableSchemaInfo) {
    return tableSchemaInfo.stream().anyMatch(TableSchemaInfo::isPrimaryKey);
  }

  private String getUniqueKey(List<TableSchemaInfo> tableSchemaInfo) {
    List<String> uniqueKeyList = tableSchemaInfo.stream()
        .filter(TableSchemaInfo::isPrimaryKey)
        .map(TableSchemaInfo::getColumn)
        .map(this::getColumnName)
        .filter(StringUtils::isNotBlank)
        .collect(Collectors.toList());
    return uniqueKeyList.isEmpty() ? "" :
        String.format("CONSTRAINT %s UNIQUE (%s)", UNIQUE_KEY_NAME
            , Joiner.on(",").join(uniqueKeyList));
  }

  private String getOlapTableColumnDataType(TableSchemaInfo info) {
    String type = info.getType();
    String length = info.getLength();
    if (ColumnType.valueOf(type.toUpperCase()) == ColumnType.STRING) {
      return "longtext";
    } else {
      return String.format("%s %s", type,
          StringUtils.isBlank(length) || "0".equals(length) ?
              "" : String.format("(%s)", length));
    }
  }

  public enum TableField {
    _ID_, _VERSION_
  }
}
