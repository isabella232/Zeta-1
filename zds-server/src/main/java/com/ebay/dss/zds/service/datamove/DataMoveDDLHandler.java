package com.ebay.dss.zds.service.datamove;

import com.ebay.dss.zds.model.DataMoveDetail;
import com.ebay.dss.zds.model.Platform;
import com.google.common.base.Splitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

import static com.ebay.dss.zds.service.datamove.TableMetaService.MetadataType.REALTIME;

@Service
public class DataMoveDDLHandler extends DataMoveQueryHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(DataMoveDDLHandler.class);

  @Autowired
  private TableMetaService tableMetaService;

  @Autowired
  private DataMoveInsertDataHandler dataMoveInsertDataHandler;

  @PostConstruct
  public void init() {
    setNextHandler(dataMoveInsertDataHandler);
  }


  @Override
  protected void generateQuery(DataMoveDetail dataMoveDetail) {
    if (!isHermesRnoPlatform(dataMoveDetail)) {
      StringBuilder content = new StringBuilder();
      List<String> tableInfo = Splitter.on(".").trimResults().omitEmptyStrings()
          .splitToList(dataMoveDetail.getHistory().getSourceTable());
      Object schema = DataMoveJobType.TD2HD.equals(DataMoveJobType.idOf(dataMoveDetail.getHistory().getType())) ?
          tableMetaService.getTableSchema(dataMoveDetail.getHistory().getSourcePlatform().trim(),
              tableInfo.get(0), tableInfo.get(1), REALTIME) :
          getLocalFileSchema(dataMoveDetail);
      LOGGER.info("Table Schema: {}", schema);
      switch (getType(dataMoveDetail)) {
        case "TD2HD-PARQUET":
          content.append(tableMetaService.generateSparkTableDDL(schema,
              "target_table_parquet_standard",
              dataMoveDetail.getHistory().getTargetTable(),
              tableMetaService.getTD2HDTargetLocation(dataMoveDetail)));
          break;
        case "TD2HD-AVRO":
          content.append(tableMetaService.generateAvroTableDDL(
              dataMoveDetail.getHistory().getTargetTable(),
              tableMetaService.getTDBridgeAvroFullPath(dataMoveDetail)));
          break;
        case "LC2HD-CSV":
          content.append(tableMetaService.generateSparkTableDDL(schema,
              "target_table_parquet_standard",
              dataMoveDetail.getHistory().getTargetTable(),
              tableMetaService.getLC2HDTargetLocation(dataMoveDetail)));
          break;
        case "LC2HD-PARQUET":
          content.append(tableMetaService.generateSparkTableDDL(schema,
              "target_table_parquet_hdfs",
              dataMoveDetail.getHistory().getTargetTable(),
              tableMetaService.getLC2HDTargetLocation(dataMoveDetail)));
          break;
        default:
          break;
      }
      LOGGER.info("Set Table DDL: {}", content);
      dataMoveDetail.setDdl(content.toString());
    }
  }

}
