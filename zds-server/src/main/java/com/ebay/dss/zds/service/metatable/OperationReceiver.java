package com.ebay.dss.zds.service.metatable;

import com.ebay.dss.zds.common.JsonUtil;
import com.ebay.dss.zds.model.metatable.MetaTableOperation;
import com.ebay.dss.zds.model.metatable.ZetaMetaTable;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

public interface OperationReceiver {

  void create(ZetaMetaTable zetaMetaTable);

  void drop(ZetaMetaTable zetaMetaTable);

  Object query(ZetaMetaTable zetaMetaTable);

  void update(ZetaMetaTable zetaMetaTable, Object data);

  static Map<String, String> getSchemaMap(ZetaMetaTable zetaMetaTable) {
    List<MetaTableOperation.TableSchemaInfo> tableSchema = JsonUtil.fromJson(zetaMetaTable.getSchemaInfo()
        , new TypeReference<List<MetaTableOperation.TableSchemaInfo>>() {
        });
    Map<String, String> schema = Maps.newLinkedHashMap();
    tableSchema.forEach(schemaInfo -> schema.put(schemaInfo.getColumn(), schemaInfo.getType()));
    return schema;
  }
}
