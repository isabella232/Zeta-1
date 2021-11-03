package com.ebay.dss.zds.rest;

import com.ebay.dss.zds.common.DateUtil;
import com.ebay.dss.zds.exception.ToolSetCheckException;
import com.ebay.dss.zds.model.*;
import com.ebay.dss.zds.rest.annotation.AuthenticationNT;
import com.ebay.dss.zds.service.datamove.DataMoveJob;
import com.ebay.dss.zds.service.datamove.DataMoveJobType;
import com.ebay.dss.zds.service.datamove.DataMoveService;
import com.ebay.dss.zds.service.filestorage.FileType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.collections.CollectionUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by zhouhuang on Apr 26, 2018
 */
@RestController
@RequestMapping("/DataMover")
public class DataMoveController {

  @Autowired
  private DataMoveService dataMoveService;

  @Autowired
  private ModelMapper modelMapper;

  @PostMapping("/move")
  public ZetaResponse<Object> moveTD2HDJob(@RequestBody DataMoveDetail dataMoveDetail) {
    return dataMoveService.save(dataMoveDetail, DataMoveJobType.TD2HD);
  }

  @PostMapping("/move_plus")
  public ZetaResponse<Object> move(@RequestBody DataMoveDetailDto dataMoveDetailDto) {
    List<String> overrideTables = dataMoveDetailDto.getOverrideTables();
    DataMoveDetail dataMoveDetail = modelMapper.map(dataMoveDetailDto, DataMoveDetail.class);
    switch (DataMoveJobType.idOf(dataMoveDetail.getHistory().getType())) {
      case TD2HD:
        return dataMoveService.save(dataMoveDetail, DataMoveJobType.TD2HD);
      case LC2HD:
        return dataMoveService.save(dataMoveDetail, DataMoveJobType.LC2HD);
      case VDM2HD:
        ZetaResponse<Object> response = dataMoveService.save(dataMoveDetail, DataMoveJobType.VDM2HD);
        if (CollectionUtils.isNotEmpty(overrideTables)) {
          dataMoveService.sendVdmMoveOverrideEmail(dataMoveDetail, overrideTables);
        }
        return response;
      case VDMVIEW2HD:
        return dataMoveService.save(dataMoveDetail, DataMoveJobType.VDMVIEW2HD);
    }
    throw new ToolSetCheckException("Data move Job Type is not support");
  }

  @PostMapping("/move/LC2HD")
  public ZetaResponse<Object> moveLC2HDJob(@RequestBody DataMoveDetail dataMoveDetail) {
    return dataMoveService.save(dataMoveDetail, DataMoveJobType.LC2HD);
  }

  @PostMapping("/move/VDM2HD")
  public ZetaResponse<Object> moveVDM2HDJob(@RequestBody DataMoveDetail dataMoveDetail
      , @RequestParam(value = "overrideTables", required = false
      , defaultValue = "") List<String> overrideTables) {
    ZetaResponse<Object> response = dataMoveService.save(dataMoveDetail, DataMoveJobType.VDM2HD);
    if (!overrideTables.isEmpty()) {
      dataMoveService.sendVdmMoveOverrideEmail(dataMoveDetail, overrideTables);
    }
    return response;
  }

  @PutMapping("/retry/VDM2HD/{historyId}")
  public ZetaResponse<Object> retryVDM2HDJob(@PathVariable("historyId") String historyId) {
    return dataMoveService.retryVDM2HDJob(historyId);
  }

  @PostMapping("/validate/VDM2HD")
  public List validateVDM2HDJob(@RequestBody DataMoveDetail dataMoveDetail) {
    return dataMoveService.validateVDM2HDJob(dataMoveDetail);
  }

  @GetMapping("/history")
  public List<Map<String, Object>> getHistory(String nt) {
    return dataMoveService.getHistory(nt);
  }

  @GetMapping("/history/detail")
  public History getHistoryDetail(Long historyId) {
    return dataMoveService.getHistoryDetail(historyId);
  }

  @GetMapping("/getColumns")
  public Table getColumns(String platform,
                          String dbName,
                          String tblName) {
    return dataMoveService.getColumns(platform, dbName, tblName);
  }

  @Deprecated
  @GetMapping("/VDMWorkspace")
  public List<String> getVDMWorkspaces(@AuthenticationNT String nt, String platform) {
    return dataMoveService.getVDMWorkspaces(nt, platform);
  }

  @GetMapping("/VDMDatabase")
  public List<String> getVDMDatabases(@AuthenticationNT String nt, String platform
      , @RequestParam(defaultValue = "false") String isRealtime) {
    return dataMoveService.getVDMDatabases(nt, platform, Boolean.valueOf(isRealtime));
  }

  @GetMapping("/VDMTable")
  public List<String> getVDMTables(@AuthenticationNT String nt, String platform
      , String database, @RequestParam(defaultValue = "false") String isRealtime) {
    return dataMoveService.getVDMTables(platform, database, Boolean.valueOf(isRealtime));
  }

  @GetMapping("/getNextRuntime")
  public Date getNextRuntime(String crontab) {
    return DateUtil.getCronDate(crontab, new Date());
  }

  @GetMapping("/cleanFinishJob")
  public void cleanFinishJob() {
    DataMoveJob.FINISHED_JOBS.clear();
  }

  @GetMapping("/cleanUser/{nt}")
  public boolean cleanUser(@PathVariable String nt) {
    return DataMoveJob.RUNNING_USERS.remove(nt);
  }

  @PostMapping("/file/upload")
  public FileStorageResponse uploadFile(@AuthenticationNT String nt,
                                        @RequestParam("fileType") String fileType,
                                        @RequestParam("file") MultipartFile file) {
    if (!FileType.isFileSupport(fileType)) {
      return new FileStorageResponse(FileStorageResponse.FAIL_CODE
          , String.format("File Type %s is not support", fileType));
    }
    return dataMoveService.uploadFile(file, nt);
  }

  @GetMapping("/file/parse")
  public String parseHeader(String fullPath) {
    Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();
    Map<String, String> header = dataMoveService.parseHeader(fullPath);
    return Objects.nonNull(header) ? gson.toJson(header) : null;
  }

  @GetMapping("/file/list")
  public Map<String, Object> listFile(@AuthenticationNT String nt) {
    return dataMoveService.listFile(nt);
  }

  @GetMapping("/VDMView")
  public List<String> getVDMViews(@AuthenticationNT String nt, String platform
      , String database) {
    return dataMoveService.getVDMViews(nt, platform, database);
  }

}