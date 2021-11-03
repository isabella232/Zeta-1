package com.ebay.dss.zds.service.schedule;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.ebay.dss.zds.model.ZetaOperationLog.OperationType;
import com.ebay.dss.zds.dao.ZetaOperationLogRepository;
import com.ebay.dss.zds.common.BeanUtils;
import com.ebay.dss.zds.exception.AuthorizationException;
import com.ebay.dss.zds.exception.ScheduleException;
import com.ebay.dss.zds.model.authorization.AuthorizationInfo;
import com.ebay.dss.zds.model.authorization.AuthorizationInfo.AuthType;
import com.ebay.dss.zds.model.*;
import com.ebay.dss.zds.model.schedule.*;
import com.ebay.dss.zds.model.authorization.AuthorizationMail;
import com.ebay.dss.zds.service.authorization.ZetaAuthorizationService;
import com.ebay.dss.zds.service.datamove.DataMoveService;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import io.jsonwebtoken.lang.Assert;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.ebay.dss.zds.common.DateUtil;
import com.ebay.dss.zds.common.JsonUtil;
import com.ebay.dss.zds.common.ScheduleUtil;
import com.ebay.dss.zds.dao.ScheduleHistoryRepository;
import com.ebay.dss.zds.dao.ScheduleJobRepository;
import com.ebay.dss.zds.websocket.notebook.dto.ExecuteCodeReq;
import org.springframework.transaction.annotation.Transactional;
import com.ebay.dss.zds.model.schedule.ScheduleHistory.JobRunStatus;
import com.ebay.dss.zds.model.schedule.ScheduleHistory.JobOperation;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;

import static com.ebay.dss.zds.common.DateUtil.DT;
import static com.ebay.dss.zds.model.schedule.ScheduleHistory.JobRunStatus.*;
import static com.ebay.dss.zds.service.MailService.SCHEDULE_JOB_URL;
import static com.ebay.dss.zds.service.authorization.AuthorizationConstant.*;
import static com.ebay.dss.zds.service.authorization.AuthorizationType.ZETA_SCHEDULE;
import static com.ebay.dss.zds.service.schedule.ScheduleConstant.*;


/**
 * Created by zhouhuang on 2018年11月1日
 */
@Service
public class ScheduleJobService {

  private final static Logger LOGGER = LoggerFactory.getLogger(ScheduleJobService.class);

  @Autowired
  private ScheduleJobRepository scheduleJobRepository;

  @Autowired
  private ScheduleHistoryRepository scheduleHistoryRepository;

  @Autowired
  private DataMoveService dataMoveService;

  @Autowired
  private ZetaAuthorizationService zetaAuthorizationService;

  @Autowired
  private ZetaOperationLogRepository zetaOperationLogRepository;

  @Autowired
  private ModelMapper modelMapper;

  @Autowired
  private ScheduleOperationTrack operationTrack;

  @Resource(name = "error-handle-rest-template")
  private RestTemplate restTemplate;

  private static String GET_TABLE_STATUS_API;
  private static String SET_TABLE_STATUS_API;

  @Value("${doe.new.url}")
  private void initServiceUrl(String hostUrl) {
    GET_TABLE_STATUS_API = hostUrl + "/dependency/getTableStatus?platform=%s&db=%s&table=%s&date=%s";
    SET_TABLE_STATUS_API = hostUrl + "/dependency/setTableStatus";
  }

  public ScheduleJobDto create(String nt, ScheduleJob scheduleJob) {
    LOGGER.info("{} create job {}", nt, JsonUtil.toJson(scheduleJob));
    checkScheduleJobNameNotExist(scheduleJob.getJobName(), scheduleJob.getNt());
    return convertScheduleJobToDto(save(scheduleJob));
  }

  public ScheduleJob save(ScheduleJob scheduleJob) {
    Assert.notNull(scheduleJob, "ScheduleJob mustn't be null");
    // check task format
    checkScheduleTaskData(scheduleJob);
    initScheduleJob(scheduleJob.getNt(), scheduleJob);

    if (ScheduleJobType.valueOf(scheduleJob.getType()) == ScheduleJobType.DataMove
        && scheduleJob.getStatus() == 1) {
      DataMoveDetail dataMoveDetail = JsonUtil.fromJson(scheduleJob.getTask(), DataMoveDetail.class);
      dataMoveService.sendVdmMoveOverrideEmail(dataMoveDetail
          , dataMoveService.getOverrideTables(dataMoveDetail));
    }
    return scheduleJobRepository.save(scheduleJob);
  }

  private void checkScheduleJobNameNotExist(String jobName, String nt) {
    if (Objects.nonNull(jobName) &&
        !scheduleJobRepository.findByNtAndJobName(nt, jobName).isEmpty()) {
      throw new ScheduleException(String.format(SCHEDULE_JOB_NAME_EXIST, jobName));
    }
  }

  private void initScheduleJob(String nt, ScheduleJob scheduleJob) {
    setScheduleJobNextRuntime(scheduleJob);
    if (Objects.nonNull(scheduleJob.getId())) {
      scheduleJob.setUpdateTime(new Date());
    } else {
      scheduleJob.setNt(nt);
      scheduleJob.setCreateTime(new Date());
      scheduleJob.setAuthInfo(JsonUtil.toJson(zetaAuthorizationService.initAuthInfo()));
    }
    LOGGER.info("Init [{}] Schedule Job - {}", nt, JsonUtil.toJson(scheduleJob));
  }

  private void setScheduleJobNextRuntime(ScheduleJob scheduleJob) {
    Date nextRunTime = null;
    ScheduleTime scheduleTime = JsonUtil.fromJson(scheduleJob.getScheduleTime(), ScheduleTime.class);
    if (scheduleJob.getStatus() == 1) {
      if (!"ONETIME".equals(scheduleTime.getJobType())) {
        String crontab = ScheduleUtil.createCronExpression(scheduleTime);
        scheduleJob.setCronExpression(crontab);
        nextRunTime = DateUtil.getCronDate(crontab, new Date());
      } else {
        ZoneId zoneId = ZoneId.systemDefault();
        nextRunTime = Date.from(LocalDateTime.of(scheduleTime.getYear(), scheduleTime.getMonth(),
            scheduleTime.getDayOfMonths(), scheduleTime.getHour(), scheduleTime.getMinute()).atZone
            (zoneId).toInstant());
      }
    }
    LOGGER.info("Set Schedule Job {} NextRunTime as {}", scheduleJob.getId(), nextRunTime);
    scheduleJob.setNextRunTime(nextRunTime);
  }

  private void checkScheduleTaskData(ScheduleJob scheduleJob) {
    switch (ScheduleJobType.valueOf(scheduleJob.getType())) {
      case DataMove:
        DataMoveDetail dataMoveDetail = JsonUtil.fromJson(scheduleJob.getTask(), DataMoveDetail.class);
        checkTaskFieldNotEmpty(dataMoveDetail.getHistory(),
            Lists.newArrayList("nt", "sourceTable", "targetTable", "sourcePlatform", "targetPlatform"));
        break;
      case DataValidate:
        DataValidateDetail dataValidateDetail = JsonUtil.fromJson(scheduleJob.getTask(), DataValidateDetail.class);
        checkTaskFieldNotEmpty(dataValidateDetail.getHistory(),
            Lists.newArrayList("nt", "sourceTable", "targetTable", "sourcePlatform", "targetPlatform"));
        break;
      case Notebook:
        NoteBookSchedulerJob noteBookSchedulerJob = JsonUtil.fromJson(scheduleJob.getTask(), NoteBookSchedulerJob.class);
        checkNotebookTaskData(noteBookSchedulerJob);
        if (StringUtils.isNotBlank(scheduleJob.getDependency())) {
          SchedulerDependency schedulerDependency = JsonUtil.fromJson(scheduleJob.getDependency(), SchedulerDependency.class);
          if (schedulerDependency.isEnabled()) {
            checkTaskFieldNotEmpty(schedulerDependency, Lists.newArrayList("waitingHrs", "dependencyTables"));
          }
        }
        if (StringUtils.isNotBlank(scheduleJob.getDependencySignal())) {
          JsonUtil.fromJson(scheduleJob.getDependencySignal(), SchedulerDependencySignal.class);
        }
        break;
      case MetaTable:
        //todo
        break;
    }
  }

  private void checkNotebookTaskData(NoteBookSchedulerJob noteBookSchedulerJob) {
    ExecuteCodeReq req = noteBookSchedulerJob.getReq();
    checkTaskFieldNotEmpty(req, Lists.newArrayList("notebookId", "interpreter"));
    if ("livy-sparksql".equals(req.getInterpreter())) {
      checkTaskFieldNotEmpty(noteBookSchedulerJob, Lists.newArrayList("clusterId", "proxyUser"));
    } else {
      checkTaskFieldNotEmpty(noteBookSchedulerJob, Lists.newArrayList("prop", "proxyUser"));
    }
  }

  private void checkTaskFieldNotEmpty(Object object, List<String> checkedFieldNames) {
    if (Objects.isNull(object)) {
      throw new ScheduleException("Object is NULL");
    }
    Class clazz = object.getClass();

    for (String fieldName : checkedFieldNames) {
      try {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        Object value = field.get(object);
        if (Objects.isNull(value) || "".equals(value.toString())) {
          throw new ScheduleException(fieldName + " in task field is EMPTY");
        }
      } catch (NoSuchFieldException | IllegalAccessException e) {
        throw new ScheduleException(fieldName + " in task field is NOT EXIST");
      }
    }
  }

  public Map<AuthType, Set<AuthorizationInfo>> grantAuthInfo(String nt
      , Long id, Map<AuthType, Set<AuthorizationInfo>> authInfo) {
    LOGGER.info("{} grant job {} authinfo: {}", nt, id, JsonUtil.toJson(authInfo));
    zetaAuthorizationService.checkAuthInfoFormat(authInfo);
    ScheduleJob scheduleJob = findScheduleJobById(id);
    if (!zetaAuthorizationService.isWriter(nt, scheduleJob)) {
      throw new AuthorizationException(NO_ACCESS_GRANT);
    }
    if (scheduleJobRepository.updateScheduleJobAuthInfo(
        JsonUtil.toJson(authInfo), id) < 1) {
      throw new ScheduleException(String.format(SCHEDULE_JOB_NOT_EXIST, id));
    }
    // send email
    sendGrantAuthEmail(nt, scheduleJob, authInfo);
    return authInfo;
  }

  private void sendGrantAuthEmail(String nt, ScheduleJob scheduleJob
      , Map<AuthType, Set<AuthorizationInfo>> authInfo) {
    Map<AuthType, Set<AuthorizationInfo>> originAuthInfo =
        StringUtils.isBlank(scheduleJob.getAuthInfo()) ?
            zetaAuthorizationService.initAuthInfo() :
            zetaAuthorizationService.parseAuthInfo(scheduleJob.getAuthInfo());
    AuthorizationMail authorizationMail = new AuthorizationMail();
    authorizationMail.setJobName(scheduleJob.getJobName());
    authorizationMail.setAuthorizationType(ZETA_SCHEDULE);
    authorizationMail.setAuthInfo(authInfo);
    authorizationMail.setOriginAuthInfo(originAuthInfo);
    authorizationMail.setLink(String.format(SCHEDULE_JOB_URL, scheduleJob.getId()));
    zetaAuthorizationService.sendGrantEmail(nt, authorizationMail);
  }

  private ScheduleJob findScheduleJobById(Long id) {
    return scheduleJobRepository.findById(id).orElseThrow(
        () -> new ScheduleException(String.format(SCHEDULE_JOB_NOT_EXIST, id))
    );
  }

  private ScheduleHistory findScheduleHistoryById(Long id) {
    return scheduleHistoryRepository.findById(id).orElseThrow(
        () -> new ScheduleException(String.format(SCHEDULE_JOB_INSTANCE_NOT_EXIST, id))
    );
  }


  public ScheduleJobDto update(String nt, ScheduleJob updateScheduleJob) {
    LOGGER.info("{} update job {}", nt, JsonUtil.toJson(updateScheduleJob));
    ScheduleJob scheduleJob = findScheduleJobById(updateScheduleJob.getId());

    if (!zetaAuthorizationService.isWriter(nt, scheduleJob)) {
      throw new AuthorizationException(NO_ACCESS_UPDATE);
    }
    BeanUtils.merge(scheduleJob, updateScheduleJob, UPDATE_JOB_FIELDS);
    return convertScheduleJobToDto(save(scheduleJob));
  }

  public List<ScheduleJobDto> getJobList(String nt) {
    LOGGER.info("Get {} job list", nt);
    List<ScheduleJob> scheduleJobList = findAllJobByNt(nt);
    return scheduleJobList.stream()
        .map(this::convertScheduleJobToDto)
        .collect(Collectors.toList());
  }

  private List<ScheduleJob> findAllJobByNt(String nt) {
    List<ScheduleJob> scheduleJobList = scheduleJobRepository.findAll(
        Sort.by(Sort.Direction.DESC, "nextRunTime"));
    return scheduleJobList.stream()
        .filter(job -> zetaAuthorizationService.isWriter(nt, job))
        .collect(Collectors.toList());
  }

  private ScheduleJobDto convertScheduleJobToDto(ScheduleJob job) {
    ScheduleJobDto jobDto = modelMapper.map(job, ScheduleJobDto.class);
    scheduleHistoryRepository.getLastRunStatus(job.getId())
        .ifPresent(i -> jobDto.setJobRunStatusInfo(
            createJobRunStatusInfo(i, i.getJobRunStatus())));
    return jobDto;
  }

  @Deprecated
  private JobRunStatusInfo getJobRunStatus(ScheduleJob job, ScheduleHistory history) {
    if (Objects.nonNull(job.getNextRunTime())) {
      switch (history.getJobRunStatus()) {
        case SUCCESS:
        case FAIL:
        case CANCELED:
          if (isJobStatusNotStart(job)) {
            return createJobRunStatusInfo(history, NOTSTART);
          }
      }
      return createJobRunStatusInfo(history, history.getJobRunStatus());
    }
    return null;
  }

  private boolean isJobStatusNotStart(ScheduleJob job) {
    ZoneId zoneId = ZoneId.systemDefault();
    LocalDate nextRunTime = LocalDateTime.ofInstant(
        job.getNextRunTime().toInstant(), zoneId).toLocalDate();
    if (LocalDate.now().isEqual(nextRunTime)) {
      return true;
    }
    return false;
  }

  private JobRunStatusInfo createJobRunStatusInfo(ScheduleHistory history, JobRunStatus status) {
    JobRunStatusInfo info = new JobRunStatusInfo();
    info.setJobRunStatus(status);
    switch (status) {
      case PENDING:
        info.setInfo(getPendingJobStatusInfo(history));
      case WAITING:
        if (Objects.isNull(info.getInfo())) {
          info.setJobRunStatus(WAITING);
          Map<String, Object> dependencyInfo = getDependencyStatusInfo(history);
          if (!dependencyInfo.isEmpty() && dependencyInfo.containsValue(false)) {
            info.setInfo(dependencyInfo);
          }
        }
        break;
    }
    return info;
  }

  Map<String, Object> getDependencyStatusInfo(ScheduleHistory history) {
    Map<String, Object> info = Maps.newLinkedHashMap();
    ScheduleJob scheduleJob = history.getScheduleJob();
    if (StringUtils.isNotBlank(scheduleJob.getDependency())) {
      SchedulerDependency schedulerDependency = JsonUtil.fromJson(scheduleJob.getDependency(),
          SchedulerDependency.class);
      if (schedulerDependency.isEnabled()) {
        String checkPlatform = getScheduleJobPlatform(scheduleJob);
        String checkDate = DateUtil.convertDateToString(history.getStartTime(), DT);

        LOGGER.info("Check platform: {}, check date: {} - {}", checkPlatform, checkDate, scheduleJob);
        for (DependencyTable dependencyTable : schedulerDependency.getDependencyTables()) {
          info.put(dependencyTable.getTableName(),
              isJobReady(dependencyTable.getTableName(), checkPlatform, checkDate));
        }
      }
    }
    return info;
  }

  private String getScheduleJobPlatform(ScheduleJob scheduleJob) {
    NoteBookSchedulerJob noteBookSchedulerJob = JsonUtil.fromJson(
        scheduleJob.getTask(), NoteBookSchedulerJob.class);
    return SchedulerCluster.getClusterById(noteBookSchedulerJob.getClusterId()).name();
  }

  private String[] splitTableNameToArray(String table) {
    List<String> tableInfo = Splitter.on(".").trimResults().omitEmptyStrings().splitToList(table);
    String db = tableInfo.size() == 1 ? "default" : tableInfo.get(0);
    String tbl = tableInfo.size() == 2 ? tableInfo.get(1) : tableInfo.get(0);
    return new String[]{db, tbl};
  }

  private boolean isJobReady(String tblName, String platform, String date) {
    String[] tableInfo = splitTableNameToArray(tblName);
    Map<String, String> tblStatus = getDependencyTableStatus(
        platform, tableInfo[0], tableInfo[1], date);
    LOGGER.info("Check Dependency [table-{} platform-{} check date-{}] Status: {}"
        , tblName, platform, date, tblStatus);
    if (Objects.nonNull(tblStatus)) {
      return tblStatus.getOrDefault("value", "not ready")
          .equalsIgnoreCase("ready");
    }
    return false;
  }

  private Map<String, String> getDependencyTableStatus(String platform, String db, String tbl, String date) {
    String url = String.format(GET_TABLE_STATUS_API, platform, db, tbl, date);
    try {
      LOGGER.info("Call get table status api with url: {}", url);
      ResponseEntity<String> responseEntity = restTemplate.getForEntity(url, String.class);
      LOGGER.info("Call get table status api response: {}", responseEntity);
      if (responseEntity.getStatusCode() == HttpStatus.OK) {
        return JsonUtil.fromJson(responseEntity.getBody(), Map.class);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  @Retryable(maxAttempts = 5, value = Exception.class, backoff = @Backoff(delay = 120000))
  public void sendDependencyTableSignal(ScheduleHistory history) {
    ScheduleJob scheduleJob = history.getScheduleJob();
    if (StringUtils.isNotBlank(scheduleJob.getDependencySignal())) {
      SchedulerDependencySignal dependencySignal = JsonUtil.fromJson(
          scheduleJob.getDependencySignal(), SchedulerDependencySignal.class);
      if (dependencySignal.isEnabled() && !dependencySignal.getSignalTables().isEmpty()) {
        String platform = getScheduleJobPlatform(scheduleJob);
        String date = DateUtil.convertDateToString(history.getStartTime(), DT);
        List<Map<String, Object>> dependencySignalBody =
            dependencySignal.getSignalTables().stream().map(table -> {
              Map<String, Object> item = Maps.newHashMap();
              item.put("platform", platform);
              item.put("date", date);
              item.put("finishTime", DateUtil.convertDateToString(history.getEndTime()));
              String[] tableInfo = splitTableNameToArray(table);
              item.put("db", tableInfo[0]);
              item.put("table", tableInfo[1]);
              return item;
            }).collect(Collectors.toList());
        callDependencyTableSignalAPI(dependencySignalBody);
      }
    }
  }

  private void callDependencyTableSignalAPI(List<Map<String, Object>> body) {
    LOGGER.info("Call set table status api with body: {}", JsonUtil.toJson(body));
    ResponseEntity<String> responseEntity = restTemplate
        .postForEntity(SET_TABLE_STATUS_API, body, String.class);
    LOGGER.info("Call set table status api response: {}", responseEntity);
    if (responseEntity.getStatusCode() != HttpStatus.OK ||
        (int) JsonUtil.fromJson(responseEntity.getBody(), Map.class)
            .getOrDefault("status", 500) != 200) {
      throw new ScheduleException("call set table status api failed");
    }
  }

  private Map<String, Object> getPendingJobStatusInfo(ScheduleHistory history) {
    Map<String, Object> info = Maps.newLinkedHashMap();
    scheduleHistoryRepository.findByScheduleJobAndJobRunStatus(history.getScheduleJob(), RUNNING)
        .ifPresent(item -> updateInfo(info, item));
    return info.isEmpty() ? null : info;
  }

  private void updateInfo(Map<String, Object> info, ScheduleHistory scheduleHistory) {
    if (scheduleHistory.getJobRunStatus() != SUCCESS) {
      info.put("runId", scheduleHistory.getId());
      info.put("status", scheduleHistory.getJobRunStatus());
      info.put("startTime", scheduleHistory.getStartTime());
    }
  }

  public List<ScheduleJobDto> getJobListById(List<Long> ids) {
    LOGGER.info("Get job list: {}", ids);
    return scheduleJobRepository.findByIdIn(ids,
        Sort.by(Sort.Direction.DESC, "nextRunTime"))
        .stream().map(this::convertScheduleJobToDto)
        .collect(Collectors.toList());
  }

  public List<ScheduleHistoryDto> getHistoryByJobId(Long id) {
    LOGGER.info("Get job {} history", id);
    return scheduleHistoryRepository.findByJobId(id).stream()
        .map(this::convertScheduleHistoryToDto)
        .collect(Collectors.toList());
  }

  private ScheduleHistoryDto convertScheduleHistoryToDto(ScheduleHistory history) {
    ScheduleHistoryDto historyDto = modelMapper.map(history, ScheduleHistoryDto.class);
    historyDto.setJobId(history.getScheduleJob().getId());
    historyDto.setJobRunStatusInfo(createJobRunStatusInfo(history, history.getJobRunStatus()));
    return historyDto;
  }

  public List<ScheduleHistoryDto> getTodayRunList(String nt) {
    LOGGER.info("{} get today run list", nt);
    List<Long> jobIdList = findAllJobByNt(nt).stream()
        .map(ScheduleJob::getId).collect(Collectors.toList());
    LOGGER.info("Today jobId list: {}", jobIdList);
    LOGGER.info("{}", scheduleHistoryRepository.getTodayRunList(jobIdList).size());
    return scheduleHistoryRepository.getTodayRunList(jobIdList).stream()
        .map(this::convertScheduleHistoryToDto).collect(Collectors.toList());
  }

  private static Pattern PATTERN_1;
  private static Pattern PATTERN_2;

  static {
    PATTERN_1 = Pattern.compile("((?<=\\$\\{).*?(?=\\}))");
    PATTERN_2 = Pattern.compile("((?<=\\$)[A-Za-z]*?(?=\\s|;))");
  }

  public String parseNotebookContent(ZetaNotebook notebook) {
    if (StringUtils.isBlank(notebook.getContent())) {
      return "";
    }
    StringBuffer content = new StringBuffer(notebook.getContent());
    if (Objects.nonNull(notebook.getPreference())) {
      try {
        Map<String, String> variables = JsonPath.read(notebook.getPreference(),
            "$['notebook.variables']");
        replaceVariables(PATTERN_1, content, variables);
        replaceVariables(PATTERN_2, content, variables);
      } catch (PathNotFoundException | IllegalStateException e) {
        LOGGER.info("No Variables In Notebook [{}]", notebook.getId());
      }
    }
    LOGGER.info("Notebook Content: \n{}", content);
    return content.toString();
  }

  private void replaceVariables(Pattern pattern, StringBuffer content,
                                Map<String, String> variables) {
    Matcher matcher = pattern.matcher(content);
    while (matcher.find()) {
      String key = matcher.group();
      if (variables.containsKey(key)) {
        if (pattern.equals(PATTERN_1)) {
          content.replace(matcher.start() - 2, matcher.end() + 1, variables.get(key));
        } else {
          content.replace(matcher.start() - 1, matcher.end(), variables.get(key));
        }
        matcher.reset();
      }
    }
  }

  @Async("schedulerTaskExecutor")
  public void deleteScheduleJob(String noteId, String nt) {
    LOGGER.info("Start to clean schedule job and history on {} for {}", noteId, nt);
    ScheduleJob scheduleJob = scheduleJobRepository.findByNtAndTaskContaining(nt, noteId);
    if (Objects.nonNull(scheduleJob)) {
      scheduleJobRepository.delete(scheduleJob);
    }
  }

  @Transactional
  public ZetaResponse deleteScheduleJob(String nt, Long id) {
    LOGGER.info("{} delete job {}", nt, id);
    ScheduleJob scheduleJob = findScheduleJobById(id);

    if (!zetaAuthorizationService.isOwner(nt, scheduleJob)) {
      throw new AuthorizationException(NO_ACCESS_DELETE);
    }

    checkExistJobInRunning(scheduleJob);
    scheduleJobRepository.delete(scheduleJob);
    return new ZetaResponse<Object>("Success", HttpStatus.OK);
  }

  private void checkExistJobInRunning(ScheduleJob job) {
    List<ScheduleHistory> scheduleHistories = scheduleHistoryRepository
        .findByScheduleJobAndJobRunStatusIn(job, Lists.newArrayList(RUNNING));
    if (!scheduleHistories.isEmpty()) {
      throw new ScheduleException(String.format(SCHEDULE_JOB_RUNNING_DENIED
          , scheduleHistories.get(0).getId()));
    }
  }

  public Optional<ScheduleJob> findScheduleJobByTask(String task) {
    return scheduleJobRepository.findOneByTask(task);
  }

  public ZetaResponse operateJob(String nt, Long id, Long runId, String operation) {
    LOGGER.info("{} {} Schedule Job Instance [{}]", nt, operation, runId);
    ScheduleHistory history = findScheduleHistoryById(runId);
    Assert.notNull(history.getScheduleJob(), String.format(SCHEDULE_JOB_NOT_EXIST, id));
    if (!zetaAuthorizationService.isWriter(nt, history.getScheduleJob())) {
      throw new AuthorizationException(NO_ACCESS_OPERATE);
    }
    history.setJobOperation(JobOperation.valueOf(operation));
    switch (JobOperation.valueOf(operation)) {
      case RUN:
      case SKIP:
        checkExistJobInRunning(history.getScheduleJob());
        history.setJobRunStatus(RUNNING);
        history.setRunTime(new Date());
        history.setEndTime(null);
        history.setLog(null);
        history.setJobHistoryId(null);
        break;
      case CANCEL:
        history.setJobRunStatus(CANCELED);
        Optional.ofNullable(history.getRunTime())
            .ifPresent(i -> history.setEndTime(new Date()));
        history.setLog(SCHEDULE_JOB_CANCELED);
        history.setJobHistoryId(null);
        break;
      default:
        throw new ScheduleException(String.format(OPERATION_DENIED, operation));
    }
    scheduleHistoryRepository.save(history);
    operationTrack.trackOperationLog(runId.toString(), nt, operation);
    return new ZetaResponse<Object>(history.getJobRunStatus(), HttpStatus.OK);
  }

  @Deprecated
  private void updateScheduleHistoryOperation(String operation, JobRunStatus status, String log, Long runId) {
    int res = Objects.isNull(log) ? scheduleHistoryRepository.updateJobOperation(operation, status.name(), runId) :
        scheduleHistoryRepository.updateJobOperationWithLog(operation, status.name(), log, runId);
    if (res < 1) {
      throw new ScheduleException(String.format(SCHEDULE_JOB_INSTANCE_NOT_EXIST, runId));
    }
  }

  public ZetaResponse applyAccess(String nt, Long id) {
    LOGGER.info("{} apply access to job {}", nt, id);
    ScheduleJob scheduleJob = findScheduleJobById(id);
    Map<AuthType, Set<AuthorizationInfo>> originAuthInfo =
        StringUtils.isBlank(scheduleJob.getAuthInfo()) ?
            zetaAuthorizationService.initAuthInfo() :
            zetaAuthorizationService.parseAuthInfo(scheduleJob.getAuthInfo());
    AuthorizationMail authorizationMail = new AuthorizationMail();
    authorizationMail.setOwner(scheduleJob.getNt());
    authorizationMail.setJobName(scheduleJob.getJobName());
    authorizationMail.setAuthorizationType(ZETA_SCHEDULE);
    authorizationMail.setOriginAuthInfo(originAuthInfo);
    authorizationMail.setLink(String.format(SCHEDULE_JOB_URL, scheduleJob.getId()));
    zetaAuthorizationService.sendRequestEmail(nt, authorizationMail);
    return new ZetaResponse<Object>("Success", HttpStatus.OK);
  }

  public List<ZetaOperationLog> getTrackLogs(String id) {
    LOGGER.info("Get job {} track log", id);
    return zetaOperationLogRepository.findByOperationIdAndOperationType(id,
        OperationType.SCHEDULER, Sort.by(Sort.Direction.DESC, "id"));
  }

  public List<String> getScheduleJobMailAddress(ScheduleJob scheduleJob) {
    List<String> ccAddr = scheduleJob.parseCcAddr();
    if (StringUtils.isNotBlank(scheduleJob.getAuthInfo())) {
      Map<AuthType, Set<AuthorizationInfo>> parseAuthInfo =
          zetaAuthorizationService.parseAuthInfo(scheduleJob.getAuthInfo());
      ccAddr.addAll(parseAuthInfo.get(AuthType.WRITERS).stream()
          .map(AuthorizationInfo::getEmail).collect(Collectors.toList()));
    }
    return ccAddr;
  }

}
