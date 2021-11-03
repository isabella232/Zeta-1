package com.ebay.dss.zds.service.metatable;

import com.ebay.dss.zds.model.schedule.ScheduleJob;
import com.ebay.dss.zds.model.metatable.ZetaMetaTable;
import com.ebay.dss.zds.service.schedule.ScheduleJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static com.ebay.dss.zds.service.schedule.ScheduleJobType.MetaTable;

@Service("Schedule")
public class ScheduleMetaTableReceiver implements OperationReceiver {

  private static final Logger LOGGER = LoggerFactory.getLogger(ScheduleMetaTableReceiver.class);
  private static final String ZETA_SHEET_SCHEDULER_ACCT = "zeta";

  @Autowired
  private ScheduleJobService scheduleJobService;

  @Override
  public void create(ZetaMetaTable zetaMetaTable) {
    ScheduleJob scheduleJob = initMetaTableScheduleJob(zetaMetaTable);
    LOGGER.info("Create Schedule Job for Meta Table-{}[{}]: {}"
        , zetaMetaTable.getMetaTableName(), zetaMetaTable.getId(), scheduleJob);
    scheduleJobService.save(scheduleJob);
  }

  @Override
  public void drop(ZetaMetaTable zetaMetaTable) {
    LOGGER.info("Drop Schedule Job for Meta Table-{}[{}]"
        , zetaMetaTable.getMetaTableName(), zetaMetaTable.getId());
    scheduleJobService.findScheduleJobByTask(zetaMetaTable.getId()).ifPresent(
        job -> scheduleJobService.deleteScheduleJob(ZETA_SHEET_SCHEDULER_ACCT, job.getId()));
  }

  @Override
  public Object query(ZetaMetaTable zetaMetaTable) {
    return null;
  }

  @Override
  public void update(ZetaMetaTable zetaMetaTable, Object data) {
    if (Objects.isNull(zetaMetaTable.getCron())) {
      return;
    }
    ScheduleJob scheduleJob = scheduleJobService.findScheduleJobByTask(zetaMetaTable.getId())
        .orElse(initMetaTableScheduleJob(zetaMetaTable));
    LOGGER.info("Update Schedule Job for Meta Table-{}[{}]: {}"
        , zetaMetaTable.getMetaTableName(), zetaMetaTable.getId(), scheduleJob);
    scheduleJobService.save(scheduleJob);
  }

  private ScheduleJob initMetaTableScheduleJob(ZetaMetaTable zetaMetaTable) {
    ScheduleJob scheduleJob = new ScheduleJob();
    scheduleJob.setNt(ZETA_SHEET_SCHEDULER_ACCT);
    scheduleJob.setJobName(zetaMetaTable.getMetaTableName());
    scheduleJob.setStatus(1);
    scheduleJob.setType(MetaTable.name());
    scheduleJob.setTask(zetaMetaTable.getId());
    scheduleJob.setScheduleTime(zetaMetaTable.getCron());
    LOGGER.info("Init Meta Table Schedule Job: {}", scheduleJob);
    return scheduleJob;
  }

}
