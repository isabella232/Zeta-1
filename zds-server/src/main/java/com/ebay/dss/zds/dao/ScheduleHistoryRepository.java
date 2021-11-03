package com.ebay.dss.zds.dao;

import java.util.List;
import java.util.Optional;

import com.ebay.dss.zds.model.schedule.ScheduleJob;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.ebay.dss.zds.model.schedule.ScheduleHistory;
import com.ebay.dss.zds.model.schedule.ScheduleHistory.JobRunStatus;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;


/**
 * Created by zhouhuang on 2018年11月2日
 */
@Repository
public interface ScheduleHistoryRepository extends JpaRepository<ScheduleHistory, Long> {

  @Query(value = "select sh.* from schedule_history sh inner join schedule_job sj on sh.job_id = sj.id where job_history_id=?1 and type=?2"
      , nativeQuery = true)
  ScheduleHistory findByJobHistoryIdAndType(Long jobHistoryId, String type);

  @Query(value = "select * from schedule_history where job_id=?1 order by id desc"
      , nativeQuery = true)
  List<ScheduleHistory> findByJobId(Long jobId);

  @Query(value = "SELECT * FROM `schedule_history` WHERE job_id in (:ids) " +
      "and (DATE_FORMAT(start_time,'%Y-%m-%d') =CURDATE() or job_run_status in ('PENDING','WAITING','RUNNING'))",
      nativeQuery = true)
  List<ScheduleHistory> getTodayRunList(@Param("ids") List<Long> ids);

  @Transactional
  @Modifying
  @Query(value = "update schedule_history set job_operation = ?1,job_run_status = ?2,log= ?3  where id = ?4"
      , nativeQuery = true)
  int updateJobOperationWithLog(String jobOperation, String jobRunStatus, String log, Long id);

  @Transactional
  @Modifying
  @Query(value = "update schedule_history set job_operation = ?1,job_run_status = ?2 where id = ?3"
      , nativeQuery = true)
  int updateJobOperation(String jobOperation, String jobRunStatus, Long id);

  List<ScheduleHistory> findByScheduleJobAndJobRunStatusIn(ScheduleJob scheduleJob, List<JobRunStatus> jobStatus);

  @Query(value = "select sh.* from schedule_history sh inner join schedule_job sj on sh.job_id = sj.id " +
      "where job_run_status in ('PENDING','WAITING','RUNNING','CANCELED') order by sh.id", nativeQuery = true)
  List<ScheduleHistory> getRunnableScheduleHistory();

  //  @Query(value = "select * from schedule_history where id<?1 order by id desc limit 1",
//      nativeQuery = true)
  Optional<ScheduleHistory> findFirstByScheduleJobAndIdLessThanOrderByIdDesc(ScheduleJob job, Long id);

  Optional<ScheduleHistory> findFirstByScheduleJobAndIdGreaterThanOrderById(ScheduleJob job, Long id);

  @Deprecated
  Optional<ScheduleHistory> findFirstByScheduleJobOrderByIdDesc(ScheduleJob job);

  @Query(value = "select * from schedule_history where job_id = ?1 and job_run_status in ('SUCCESS','FAIL','CANCELED') order by end_time desc limit 1"
      , nativeQuery = true)
  Optional<ScheduleHistory> getLastRunStatus(Long jobId);

  @Transactional
  @Modifying
  @Query(value = "update schedule_history set job_run_status=?1 where id = ?2", nativeQuery = true)
  int updateJobRunStatus(String jobRunStatus, Long id);

  @Query(value = "select * from schedule_history where job_id = ?1 and id <= ?2 order by id desc limit ?3 ", nativeQuery = true)
  List<ScheduleHistory> findLatestJobHistoriesByLimitTo(Long jobId, Long id, int limit);

  Optional<ScheduleHistory> findByScheduleJobAndJobRunStatus(ScheduleJob scheduleJob, JobRunStatus jobRunStatus);

  @Transactional
  @Modifying
  @Query(value = "update schedule_history set job_run_status = ?1 where id = ?2 and job_run_status != 'CANCELED'", nativeQuery = true)
  int updateJobRunStatusIfNotCancelled(String jobRunStatus, Long id);
}
