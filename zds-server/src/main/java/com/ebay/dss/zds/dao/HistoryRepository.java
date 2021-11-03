package com.ebay.dss.zds.dao;

import java.util.List;
import java.util.Map;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.ebay.dss.zds.model.History;

/**
 * Created by zhouhuang on Apr 26, 2018
 */
@Repository
public interface HistoryRepository extends JpaRepository<History, Long> {

  History getHistoryByHistoryId(Long historyId);

  @Deprecated
  @Modifying
  @Transactional
  @Query(value = "update history set crontab=null  where history_id=?1", nativeQuery = true)
  int stopCrontab(Integer historyId);

  @Query(value = "select history_id historyId,nT,type,source_table sourceTable,target_table targetTable," +
      "source_platform sourcePlatform,target_platform targetPlatform,status,log,cre_date createDate," +
      "start_time startTime,end_time endTime from history where nt = ?1 and type = ?2 order by history_id desc limit 150"
      , nativeQuery = true)
  List<Map<String, Object>> getHistoryList(String nt, int type);


  @Query(value = "select h.history_id historyId,nT,type,source_table sourceTable,target_table targetTable," +
      "source_platform sourcePlatform,target_platform targetPlatform,status,log,h.cre_date createDate," +
      "start_time startTime,end_time endTime,view_db viewDb from history h inner join data_move_detail dm " +
      "on h.history_id = dm.history_id where nt = ?1 and type = 5 order by h.history_id desc limit 150"
      , nativeQuery = true)
  List<Map<String, Object>> getVdmViewHistoryList(String nt);

  List<History> findBySourceTableAndSourcePlatformAndStatusAndTypeAndNt(String sourceTable, String sourcePlatform, int status, int type, String nT);
}
