package com.ebay.dss.zds.model.metatable;

import java.util.List;
import java.util.Map;

public class MetaTableOperation {

  Map<Operations, List<Object>> operations;
  Object metaTableInfo;

  public Map<Operations, List<Object>> getOperations() {
    return operations;
  }

  public void setOperations(Map<Operations, List<Object>> operations) {
    this.operations = operations;
  }

  public Object getMetaTableInfo() {
    return metaTableInfo;
  }

  public void setMetaTableInfo(Object metaTableInfo) {
    this.metaTableInfo = metaTableInfo;
  }

  public enum Operations {
    CREATE, REMOVE, ADD, DROP, INDEXADD, INDEXDROP, SELECT, UPDATE, DELETE, INSERT
  }

  public static class TableSchemaInfo {
    String column;
    String type;
    String length;
    String desc;
    boolean primaryKey;
    boolean nullValue;
    boolean editable;
    boolean validate;

    public String getColumn() {
      return column;
    }

    public void setColumn(String column) {
      this.column = column;
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public String getLength() {
      return length;
    }

    public void setLength(String length) {
      this.length = length;
    }

    public String getDesc() {
      return desc;
    }

    public void setDesc(String desc) {
      this.desc = desc;
    }

    public boolean isPrimaryKey() {
      return primaryKey;
    }

    public void setPrimaryKey(boolean primaryKey) {
      this.primaryKey = primaryKey;
    }

    public boolean isNullValue() {
      return nullValue;
    }

    public void setNullValue(boolean nullValue) {
      this.nullValue = nullValue;
    }

    public boolean isEditable() {
      return editable;
    }

    public void setEditable(boolean editable) {
      this.editable = editable;
    }

    public boolean isValidate() {
      return validate;
    }

    public void setValidate(boolean validate) {
      this.validate = validate;
    }
  }
}
