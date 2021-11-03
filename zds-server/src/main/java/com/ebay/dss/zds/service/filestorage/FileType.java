package com.ebay.dss.zds.service.filestorage;

import com.ebay.dss.zds.model.DataMoveDetail;
import com.google.common.base.Splitter;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

public enum FileType {

  JAR("jar"),
  PY("py"),
  R("r"),
  PARQUET("parquet"),
  CSV("csv"),
  AVRO("avro"),
  ZIP("zip"),
  EGG("egg"),
  FILE("file"),
  UNKNOW("unknow");

  private String type;

  static Map<String, FileType> typeMap = Maps.newHashMap();

  static {
    for (FileType fileType : FileType.values()) {
      typeMap.put(fileType.getType(), fileType);
    }
  }

  FileType(String type) {
    this.type = type;
  }

  public String getType() {
    return this.type;
  }

  public static FileType typeOf(String type) {
    return typeMap.get(type);
  }

  public static boolean isFileSupport(String type) {
    if (type == null) {
      return false;
    }
    return typeMap.keySet().contains(type.toLowerCase());
  }

  public static FileType getFileTypeByPath(String filePath) {
    List<String> paths = Splitter.on(".").trimResults().splitToList(filePath.toLowerCase());
    return paths.size() > 0 ? typeMap.getOrDefault(paths.get(paths.size() - 1), UNKNOW) : UNKNOW;
  }

  public static FileType getFileTypeByTD2HDJob(DataMoveDetail dataMoveDetail) {
    return dataMoveDetail.isConvert() ? FileType.PARQUET : FileType.AVRO;
  }
}
