package com.ebay.dss.zds.service.datamove;

import com.ebay.dss.zds.interpreter.InterpreterManager;
import com.ebay.dss.zds.model.*;
import com.ebay.dss.zds.service.filestorage.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;


@Component
public class LC2HDJob extends DataMoveJob {

  private static final Logger LOGGER = LoggerFactory.getLogger(LC2HDJob.class);

  @Autowired
  private TableMetaService tableMetaService;

  @Autowired
  private DataMoveTempViewHandler dataMoveTempViewHandler;

  @Override
  void initialize(DataMoveDetail dataMoveDetail) {
    generateLC2HDQuery(dataMoveDetail);
  }

  public void generateLC2HDQuery(DataMoveDetail dataMoveDetail) {
    if (dataMoveDetail.getHistory().getStatus() == 0 && dataMoveDetail.getStep() == 0) {
      dataMoveTempViewHandler.handle(dataMoveDetail);
      LOGGER.info("{} Generate TD to HD Move Queries [{}]", dataMoveDetail, getSparkSQL(dataMoveDetail));
      dataMoveDetail.setStep(1);
    }
  }

  @Override
  void startMove(DataMoveDetail dataMoveDetail) {
    LOGGER.info("LC2HDJob {} Begin to Move", dataMoveDetail);

    UnaryOperator<DataMoveDetail> stepOneProcessing = (DataMoveDetail dmd) -> runStepOne(dmd);
    UnaryOperator<DataMoveDetail> stepTwoProcessing = (DataMoveDetail dmd) -> runStepTwo(dmd);
    UnaryOperator<DataMoveDetail> stepThreeProcessing = (DataMoveDetail dmd) -> runStepThree(dmd);
    Function<DataMoveDetail, DataMoveDetail> pipeline = stepOneProcessing
        .andThen(stepTwoProcessing)
        .andThen(stepThreeProcessing);
    pipeline.apply(dataMoveDetail);

  }

  public DataMoveDetail runStepTwo(DataMoveDetail dataMoveDetail) {
    if (dataMoveDetail.getHistory().getStatus() == 0 && dataMoveDetail.getStep() == 2) {
      LOGGER.info("{} Move Step 2", dataMoveDetail);
      String nt = dataMoveDetail.getHistory().getNt();
      Platform platform = Platform.valueOf(dataMoveDetail.getHistory().getTargetPlatform().trim());
      JobResult jobResult;
      switch (platform) {
        case hermes:
        default:
          // upload file to hdfs
          jobResult = hadoopTableOperation.uploadFileToHdfs(platform, nt
              , getUploadLocalFileFullPath(dataMoveDetail)
              , tableMetaService.getFileUploadHDFSPath(dataMoveDetail)
              , dataMoveDetail.getHistory().getSourceTable());
          jobResult = jobResult.getStatus() ?
              hadoopTableOperation.executeSparkSQL(
                  nt, platform, dataMoveDetail.getQueue(), getSparkSQL(dataMoveDetail))
              : jobResult;
          break;
      }
      LOGGER.info("{} Step 2 Execute Result [{}]", dataMoveDetail, jobResult);

      if (jobResult.getStatus()) {
        if (FileType.getFileTypeByPath(dataMoveDetail.getHistory().getSourceTable()) == FileType.CSV
            && platform.getId() != Platform.hermes.getId()) {
          //csv file on HDFS need to delete
          dataMoveDetail.setStep(3);
        } else {
          dataMoveDetail.getHistory().setStatus(1);
        }
      } else {
        dataMoveDetail.getHistory().setStatus(2);
        dataMoveDetail.setErrorLog(jobResult.getOutput());
      }
    }
    return dataMoveDetail;
  }

  private String getUploadLocalFileFullPath(DataMoveDetail dataMoveDetail) {
    return dataMoveDetail.getHistory().getSourcePlatform().trim() +
        dataMoveDetail.getHistory().getSourceTable().trim();
  }

  public DataMoveDetail runStepThree(DataMoveDetail dataMoveDetail) {
    if (dataMoveDetail.getHistory().getStatus() == 0 && dataMoveDetail.getStep() == 3) {
      LOGGER.info("{} Move Step 3", dataMoveDetail);
      Platform platform = Platform.valueOf(dataMoveDetail.getHistory().getTargetPlatform().trim());
      String nt = dataMoveDetail.getHistory().getNt();
      String hdfsPath = tableMetaService.getFileUploadHDFSPath(dataMoveDetail);
      hadoopTableOperation.deleteHdfsFile(platform, nt, hdfsPath
          , dataMoveDetail.getHistory().getSourceTable());
      dataMoveDetail.getHistory().setStatus(1);
    }
    return dataMoveDetail;
  }

}
