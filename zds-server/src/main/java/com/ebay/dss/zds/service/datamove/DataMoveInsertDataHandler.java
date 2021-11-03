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
import java.util.Optional;

import static com.ebay.dss.zds.service.datamove.TableMetaService.MetadataType.REALTIME;

@Service
public class DataMoveInsertDataHandler extends DataMoveQueryHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(DataMoveInsertDataHandler.class);

  @Autowired
  private TableMetaService tableMetaService;

  @Autowired
  private DataMoveHermesDataHandler dataMoveHermesDataHandler;


  @PostConstruct
  public void init() {
    setNextHandler(dataMoveHermesDataHandler);
  }


  @Override
  protected void generateQuery(DataMoveDetail dataMoveDetail) {
    if (!isHermesRnoPlatform(dataMoveDetail)) {

      StringBuilder content = new StringBuilder(Optional.ofNullable(dataMoveDetail.getSparkSql())
          .orElse(""));
      switch (getType(dataMoveDetail)) {
        case "TD2HD-PARQUET":
          List<String> tableInfo = Splitter.on(".").trimResults().omitEmptyStrings()
              .splitToList(dataMoveDetail.getHistory().getSourceTable());
          Object schema = tableMetaService.getTableSchema(dataMoveDetail.getHistory().getSourcePlatform().trim(),
              tableInfo.get(0), tableInfo.get(1), REALTIME);
          content.append("\n").append(tableMetaService.genInsertQuery(dataMoveDetail.getHistory().getTargetTable(),
              tableMetaService.getTempViewName(dataMoveDetail),
              schema));
          break;
        case "LC2HD-CSV":
          content.append("\n").append(tableMetaService.genInsertQuery(dataMoveDetail.getHistory().getTargetTable(),
              tableMetaService.getTempViewName(dataMoveDetail)));
          break;
        default:
          break;
      }
      LOGGER.info("Set Spark SQL: {}", content);
      dataMoveDetail.setSparkSql(content.toString());
    }
  }

}
