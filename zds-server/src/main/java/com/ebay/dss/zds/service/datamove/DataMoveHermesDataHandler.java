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
public class DataMoveHermesDataHandler extends DataMoveQueryHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(DataMoveHermesDataHandler.class);

  @Autowired
  private TableMetaService tableMetaService;


  @PostConstruct
  public void init() {
    setNextHandler(null);
  }

  private static final String GLOBAL_DB = "global_temp";


  @Override
  protected void generateQuery(DataMoveDetail dataMoveDetail) {
    if (isHermesRnoPlatform(dataMoveDetail)) {
      generateDDL(dataMoveDetail);
//      generateInsertQuery(dataMoveDetail, generateTempView(dataMoveDetail));
    }
  }

  @Deprecated
  private StringBuilder generateTempView(DataMoveDetail dataMoveDetail) {
    String viewName = tableMetaService.getTempViewName(dataMoveDetail);
    StringBuilder content = new StringBuilder("set role admin;\n");
    switch (getType(dataMoveDetail)) {
      case "LC2HD-PARQUET":
        content.append(tableMetaService.genTempView(viewName,
            tableMetaService.getLC2HDTargetLocation(dataMoveDetail),
            FileType.PARQUET, false));
        break;
      case "LC2HD-CSV":
        content.append(tableMetaService.genTempView(viewName,
            tableMetaService.getFileUploadHDFSFullPath(dataMoveDetail),
            FileType.CSV, false));
        break;
    }
//      content.append(String.format("GRANT SELECT ON %s.%s TO USER %s;", GLOBAL_DB,
//          viewName, dataMoveDetail.getHistory().getnT()));
    LOGGER.info("Hermes View SQL: {}", content);
    return content;
  }

  private void generateDDL(DataMoveDetail dataMoveDetail) {
    Object schema = getLocalFileSchema(dataMoveDetail);
    dataMoveDetail.setDdl(tableMetaService.generateHermesTableDDL(schema,
        dataMoveDetail.getHistory().getTargetTable()));
    LOGGER.info("Hermes DDL: {}", dataMoveDetail.getDdl());
  }

  @Deprecated
  private void generateInsertQuery(DataMoveDetail dataMoveDetail,
                                   StringBuilder tempViewSQL) {
    String viewName = tableMetaService.getTempViewName(dataMoveDetail);
    tempViewSQL.append(tableMetaService.genInsertQuery(
        dataMoveDetail.getHistory().getTargetTable(),
        viewName));
    dataMoveDetail.setSparkSql(tempViewSQL.toString());
    LOGGER.info("Hermes Spark SQL: {}", dataMoveDetail.getSparkSql());
  }

}
