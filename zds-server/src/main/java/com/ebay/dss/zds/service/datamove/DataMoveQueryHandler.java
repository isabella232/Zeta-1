package com.ebay.dss.zds.service.datamove;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.ebay.dss.zds.model.DataMoveDetail;
import com.ebay.dss.zds.model.Platform;
import com.ebay.dss.zds.service.filestorage.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;


public abstract class DataMoveQueryHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(DataMoveQueryHandler.class);

  protected DataMoveQueryHandler nextDataMoveQueryHandler;

  public void setNextHandler(DataMoveQueryHandler dataMoveQueryHandler) {
    this.nextDataMoveQueryHandler = dataMoveQueryHandler;
  }

  public void handle(DataMoveDetail dataMoveDetail) {
    generateQuery(dataMoveDetail);
    if (Objects.nonNull(nextDataMoveQueryHandler)) {
      nextDataMoveQueryHandler.handle(dataMoveDetail);
    }
  }

  abstract protected void generateQuery(DataMoveDetail dataMoveDetail);

  protected String getType(DataMoveDetail dataMoveDetail) {
    DataMoveJobType jobType = DataMoveJobType.idOf(dataMoveDetail.getHistory().getType());
    FileType fileType = DataMoveJobType.TD2HD.equals(jobType) ?
        FileType.getFileTypeByTD2HDJob(dataMoveDetail)
        : FileType.getFileTypeByPath(dataMoveDetail.getHistory().getSourceTable());
    return String.format("%s-%s", jobType, fileType);
  }

  protected Map getLocalFileSchema(DataMoveDetail dataMoveDetail) {
    Map schema = JSON.parseObject(dataMoveDetail.getDdl(), LinkedHashMap.class, Feature.OrderedField);
    return schema;
  }

  protected boolean isHermesRnoPlatform(DataMoveDetail dataMoveDetail) {
    Platform targetPlatform = Platform.valueOf(dataMoveDetail.getHistory().getTargetPlatform());
    return targetPlatform.getId() == Platform.hermes.getId();
  }

}
