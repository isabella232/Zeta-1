package com.ebay.dss.zds.rest;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.ebay.dss.zds.model.ZetaOperationLog;
import com.ebay.dss.zds.model.authorization.AuthorizationInfo;
import com.ebay.dss.zds.model.authorization.AuthorizationInfo.AuthType;
import com.ebay.dss.zds.model.ZetaResponse;
import com.ebay.dss.zds.model.schedule.ScheduleHistoryDto;
import com.ebay.dss.zds.rest.annotation.AuthenticationNT;
import com.ebay.dss.zds.model.schedule.ScheduleJobDto;
import com.ebay.dss.zds.service.schedule.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.ebay.dss.zds.model.schedule.ScheduleJob;
import com.ebay.dss.zds.service.schedule.ScheduleJobService;

/**
 * Created by zhouhuang
 */
@RestController
@RequestMapping("/Scheduler")
public class SchedulerController {


  @Autowired
  private ScheduleJobService scheduleJobService;

  @PostMapping("/create")
  public ScheduleJobDto createJob(
      @AuthenticationNT String nt
      , @RequestBody ScheduleJob scheduleJob) {
    return scheduleJobService.create(nt, scheduleJob);
  }

  @PutMapping("/grant/{id}")
  public Map<AuthType, Set<AuthorizationInfo>> grantAuth(
      @AuthenticationNT String nt
      , @PathVariable("id") Long id
      , @RequestBody Map<AuthType, Set<AuthorizationInfo>> authInfo) {
    return scheduleJobService.grantAuthInfo(nt, id, authInfo);
  }

  @PutMapping("/update")
  public ScheduleJobDto updateJob(@AuthenticationNT String nt
      , @RequestBody ScheduleJob scheduleJob) {
    return scheduleJobService.update(nt, scheduleJob);
  }

  @GetMapping("/getJobList")
  public List<ScheduleJobDto> getJobList(
      @AuthenticationNT String nt) {
    return scheduleJobService.getJobList(nt);
  }

  @GetMapping("/getJobListById")
  public List<ScheduleJobDto> getJobListByIds(@RequestParam List<Long> id) {
    return scheduleJobService.getJobListById(id);
  }

  @GetMapping("/getHistory/{id}")
  public List<ScheduleHistoryDto> getJobHistoryById(@PathVariable("id") Long id) {
    return scheduleJobService.getHistoryByJobId(id);
  }

  @GetMapping("/getTodayRunList")
  public List<ScheduleHistoryDto> getTodayRunList(
      @AuthenticationNT String nt) {
    return scheduleJobService.getTodayRunList(nt);
  }

  @DeleteMapping("/delete/{id}")
  public ZetaResponse deleteJob(@AuthenticationNT String nt
      , @PathVariable("id") Long id) {
    return scheduleJobService.deleteScheduleJob(nt, id);
  }

  @PutMapping("/{id}/{operation}")
  public ZetaResponse operateJob(@AuthenticationNT String nt
      , @PathVariable("id") Long id
      , @PathVariable("operation") String operation
      , @RequestParam(value = "runId") Long runId) {
    return scheduleJobService.operateJob(nt, id, runId, operation);
  }

  @PutMapping("/applyAccess/{nt}/{id}")
  public ZetaResponse applyAccess(@PathVariable("nt") String nt
      , @PathVariable("id") Long id) {
    return scheduleJobService.applyAccess(nt, id);
  }

  @GetMapping("/getTrackLog/{id}")
  public List<ZetaOperationLog> getTrackLogs(@PathVariable("id") String id) {
    return scheduleJobService.getTrackLogs(id);
  }

  @GetMapping("/cleanFinishJobs")
  public boolean cleanFinishJobs(String id) {
    if (Objects.nonNull(id)) {
      return Scheduler.FINISH_JOBS.remove(id);
    } else {
      Scheduler.FINISH_JOBS.clear();
      return true;
    }
  }
}
