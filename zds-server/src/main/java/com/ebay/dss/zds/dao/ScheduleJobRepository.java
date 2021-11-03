package com.ebay.dss.zds.dao;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.ebay.dss.zds.model.schedule.ScheduleJob;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by zhouhuang on 2018年11月1日
 */
@Repository
public interface ScheduleJobRepository extends JpaRepository<ScheduleJob, Long> {

  @Query(value = "select * from schedule_job where TIMESTAMPDIFF(SECOND,next_run_time,?1)>0 " +
      "and status = 1 ORDER BY type,next_run_time", nativeQuery = true)
  List<ScheduleJob> findExecutableJob(String time);

  ScheduleJob findOneById(Long id);

  Optional<ScheduleJob> findOneByTask(String task);

  List<ScheduleJob> findByStatus(int status);

  ScheduleJob findByNtAndTaskContaining(String nt, String noteId);

  List<ScheduleJob> findByNtAndJobName(String nt, String jobName);

  List<ScheduleJob> findByIdIn(List<Long> ids, Sort sort);

  @Transactional
  @Modifying
  @Query(value = "update schedule_job set auth_info=?1 where id = ?2", nativeQuery = true)
  int updateScheduleJobAuthInfo(String authInfo, Long id);

  @Query(value = "select sj.id,sj.job_name,nt,count(1) num from schedule_job sj " +
      "inner join schedule_history sh on sj.id = sh.job_id " +
      "where sj.status = 1 and sh.job_run_status in ('PENDING','WAITING') " +
      "group by sj.id,sj.job_name having count(1) > 50", nativeQuery = true)
  List<Map<String, Object>> getInactiveSchedulerJobs();

  @Transactional
  @Modifying
  @Query(value = "update schedule_job set status=?1 where id = ?2", nativeQuery = true)
  int updateScheduleJobStatus(int status, Long id);
}
