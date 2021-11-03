package com.ebay.dss.zds.service.metatable;

import com.google.common.collect.Lists;

import java.util.List;

public class MetaTableConstant {

  static final String DUPLICATE_COLUMN = "Table contains duplicate column";
  static final String SYNCING_INFO = "The zeta sheet is syncing. Please wait patiently";
  static final String META_TABLE_NOT_EXIST =
      "Zeta Sheet %s not exist. Please refresh page to get the latest data.";
  static final String GET_WHITELIST_QUERY = "select nt from zeta_sheet_whitelist;";
  static final List<String> UPDATE_TABLE_FIELDS
      = Lists.newArrayList("metaTableName", "cron");

  static final List<String> REGISTER_TABLE_FIELDS
      = Lists.newArrayList("platform", "account", "db", "tbl", "metaTableType");
  static final List<String> PUSH_TABLE_FIELDS
      = Lists.newArrayList("metaTableStatus", "hadoopSchemaInfo", "syncTime", "failLog");
}
