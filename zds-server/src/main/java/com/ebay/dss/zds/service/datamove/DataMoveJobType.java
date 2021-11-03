package com.ebay.dss.zds.service.datamove;

import com.google.common.collect.Maps;

import java.util.Map;

public enum DataMoveJobType {

  TD2HD(1), LC2HD(3), VDM2HD(4), VDMVIEW2HD(5);
  private int id;

  DataMoveJobType(int id) {
    this.id = id;
  }

  public int getId() {
    return this.id;
  }


  static Map<Integer, DataMoveJobType> dataMoveJobTypeMap = Maps.newHashMap();

  static {
    for (DataMoveJobType dataMoveJobType : DataMoveJobType.values()) {
      dataMoveJobTypeMap.put(dataMoveJobType.getId(), dataMoveJobType);
    }
  }

  public static DataMoveJobType idOf(int id) {
    return dataMoveJobTypeMap.get(id);
  }

}
