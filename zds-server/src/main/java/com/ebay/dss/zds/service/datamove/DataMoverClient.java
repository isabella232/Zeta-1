package com.ebay.dss.zds.service.datamove;

import com.ebay.dss.zds.common.FileHelper;
import com.ebay.dss.zds.model.History;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ebay.dss.zds.common.DateUtil;
import com.ebay.dss.zds.common.PropertiesUtil;
import com.ebay.dss.zds.dao.DataMoveRepository;
import com.ebay.dss.zds.model.DataMoveDetail;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Component
public class DataMoverClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(DataMoverClient.class);

  @Autowired
  private DataMoveRepository dataMoveRepository;

  @Autowired
  private TD2HDJob td2HDJob;

  @Autowired
  private LC2HDJob lc2HDJob;

  @Autowired
  private VDM2HDJob vdm2HDJob;

  @Autowired
  private VDMVIEW2HDJob vdmview2HDJob;

  @Resource(name = "dataMoveTaskExecutor")
  private Executor executor;

  public void executeDataMove() {
    LOGGER.info("Begin to check data move upload task status [{}]", System.currentTimeMillis());
    List<DataMoveDetail> dataMoveDetails = dataMoveRepository.findByHistory_Status(0);
    groupDatamoveJobList(dataMoveDetails);
  }

  public void groupDatamoveJobList(List<DataMoveDetail> dataMoveDetails) {
    Map<String, List<DataMoveDetail>> dataMoveDetailsGroup = dataMoveDetails.stream()
        .collect(Collectors.groupingBy(job -> job.getHistory().getNt()));
    dataMoveDetailsGroup.values()
        .stream()
        .filter(dataMoveDetailList -> dataMoveDetailList.size() > 0)
        .forEach(dataMoveDetailList -> submitDatamoveJob(dataMoveDetailList));

    int overflowNums = DataMoveJob.FINISHED_JOBS.size() - 2;
    if (overflowNums > 0) {
      LOGGER.info("Remove finished job: [ {} ]", DataMoveJob.FINISHED_JOBS);
      DataMoveJob.FINISHED_JOBS.removeAll(DataMoveJob.FINISHED_JOBS.subList(0, overflowNums));
    }
  }

  public void submitDatamoveJob(List<DataMoveDetail> dataMoveDetailList) {
    LOGGER.info("Start to submit datamove job {}", dataMoveDetailList);
    Map<Integer, List<DataMoveDetail>> dataMoveDetailsGroup = dataMoveDetailList.stream()
        .collect(Collectors.groupingBy(job -> job.getHistory().getType()));
    for (Map.Entry<Integer, List<DataMoveDetail>> dataMoveDetails : dataMoveDetailsGroup.entrySet()) {
      if (dataMoveDetails.getValue().size() > 0) {
        switch (DataMoveJobType.idOf(dataMoveDetails.getKey())) {
          case TD2HD:
            executor.execute(() -> td2HDJob.move(dataMoveDetails.getValue()));
            break;
          case LC2HD:
            executor.execute(() -> lc2HDJob.move(dataMoveDetails.getValue()));
            break;
          case VDM2HD:
            executor.execute(() -> vdm2HDJob.move(dataMoveDetails.getValue()));
            break;
          case VDMVIEW2HD:
            executor.execute(() -> vdmview2HDJob.move(dataMoveDetails.getValue()));
        }
      }
    }
  }

  public void executeDataMoveCronTabTask() {
    LOGGER.info("Begin to check scheduled data move task status [{}]", System.currentTimeMillis());
    List<DataMoveDetail> dataMoveDetails = dataMoveRepository.findCrontabTask();
    dataMoveDetails.forEach(dataMoveDetail -> crontabTask(dataMoveDetail));
  }

  public void crontabTask(DataMoveDetail dataMoveDetail) {
    Date curTime = new Date();
    History history = dataMoveDetail.getHistory();
    boolean needNewTask = DateUtil.compareCrontab(
        history.getCrontab(), curTime, history.getLastRunDate());
    if (needNewTask) {
      LOGGER.info("Begin to add a new scheduled task [{}]", System.currentTimeMillis());
      dataMoveDetail.getHistory().setLastRunDate(curTime);
      dataMoveRepository.save(dataMoveDetail);
      dataMoveDetail.setId(0L);
      dataMoveDetail.setTaskId(0);
      dataMoveDetail.setTouchfileId(0);
      dataMoveDetail.setCreateDate(curTime);
      dataMoveDetail.getHistory().setHistoryId(0L);
      dataMoveDetail.getHistory().setStatus(0);
      dataMoveDetail.getHistory().setLog(null);
      dataMoveDetail.getHistory().setCrontab(null);
      dataMoveDetail.getHistory().setCreateDate(curTime);
      dataMoveDetail.getHistory().setLastRunDate(null);
      dataMoveRepository.save(dataMoveDetail);
    }
  }

  public void cleanLocalFile() {
    LOGGER.info("Begin to clean local file of data move [{}]", System.currentTimeMillis());
    List<Map<String, String>> fileLists = FileHelper.getFileList(
        new File(PropertiesUtil.getDatamoveProperties("upload.folder")
            .replace("%s/", "")));
    fileLists.stream().filter(file ->
        (System.currentTimeMillis() - Long.parseLong(file.get("createTime"))) / 3600000 > 24)
        .forEach(file -> {
          LOGGER.info("Delete File {}", file.get("filePath") + file.get("fileName"));
          FileHelper.deleteFile(file.get("filePath") + file.get("fileName"));
        });
  }

}