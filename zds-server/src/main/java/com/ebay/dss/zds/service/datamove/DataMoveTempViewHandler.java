package com.ebay.dss.zds.service.datamove;

import com.ebay.dss.zds.model.DataMoveDetail;
import com.ebay.dss.zds.model.Platform;
import com.ebay.dss.zds.service.filestorage.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class DataMoveTempViewHandler extends DataMoveQueryHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(DataMoveTempViewHandler.class);

  @Autowired
  private TableMetaService tableMetaService;

  @Autowired
  private DataMoveDDLHandler dataMoveDDLHandler;

  @PostConstruct
  public void init() {
    setNextHandler(dataMoveDDLHandler);
  }

  @Override
  protected void generateQuery(DataMoveDetail dataMoveDetail) {
    if (!isHermesRnoPlatform(dataMoveDetail)) {

      String viewName = tableMetaService.getTempViewName(dataMoveDetail);
      StringBuilder content = new StringBuilder();
      switch (getType(dataMoveDetail)) {
        case "TD2HD-PARQUET":
          content.append(tableMetaService.genTempView(viewName,
              dataMoveDetail.getTdBridgeAvro(), FileType.AVRO, false));
          break;
        case "LC2HD-CSV":
          content.append(tableMetaService.genTempView(viewName,
              tableMetaService.getFileUploadHDFSFullPath(dataMoveDetail),
              FileType.CSV, false));
          break;
        default:
          break;
      }
      LOGGER.info("Set Spark SQL: {}", content);
      dataMoveDetail.setSparkSql(content.toString());
    }
  }


}
