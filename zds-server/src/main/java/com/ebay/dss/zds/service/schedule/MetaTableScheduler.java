package com.ebay.dss.zds.service.schedule;

import com.ebay.dss.zds.dao.ZetaMetaTableRepository;
import com.ebay.dss.zds.model.schedule.ScheduleHistory;
import com.ebay.dss.zds.model.schedule.ScheduleJob;
import com.ebay.dss.zds.model.metatable.ZetaMetaTable;
import com.ebay.dss.zds.service.metatable.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Optional;

import static com.ebay.dss.zds.model.schedule.ScheduleHistory.JobRunStatus.FAIL;

@Component
public class MetaTableScheduler extends Scheduler {

  private static final Logger LOGGER = LoggerFactory.getLogger(MetaTableScheduler.class);

  @Autowired
  private ZetaMetaTableRepository zetaMetaTableRepository;

  @Autowired
  private OLAPMetaTableReceiver olapMetaTableReceiver;

  @Autowired
  private HadoopMetaTableReceiver hadoopMetaTableReceiver;

  private OperationInvoker operationInvoker = new OperationInvoker();

  private QueryCommand queryCommand;

  private UpdateCommand updateCommand;

  @PostConstruct
  public void loadCommand() {
    queryCommand = new QueryCommand(olapMetaTableReceiver);
    updateCommand = new UpdateCommand(hadoopMetaTableReceiver);
  }

  @Override
  void startJob(ScheduleHistory scheduleHistory) {
    ScheduleJob scheduleJob = scheduleHistory.getScheduleJob();
    LOGGER.info("Thread [{}],Start to handle Meta Table {}",
        Thread.currentThread().getName(), scheduleJob);
    Optional<ZetaMetaTable> zetaMetaTable = zetaMetaTableRepository.findById(scheduleJob.getTask());
    if (zetaMetaTable.isPresent()) {
      Object data = operationInvoker.action(queryCommand, zetaMetaTable.get());
      operationInvoker.action(updateCommand, zetaMetaTable.get(), data);
    }
  }

  @Override
  void cancelJob(ScheduleHistory scheduleHistory) {
    //todo
  }

  String getJobFailLog(ScheduleHistory scheduleHistory) {
    //todo
    return null;
  }
}
