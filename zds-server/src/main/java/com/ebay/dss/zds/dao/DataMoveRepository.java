package com.ebay.dss.zds.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.ebay.dss.zds.model.DataMoveDetail;
import org.springframework.stereotype.Repository;

/**
 * Created by zhouhuang on Apr 26, 2018
 */
@Repository
public interface DataMoveRepository extends JpaRepository<DataMoveDetail, Long> {

	List<DataMoveDetail> findByHistory_StatusAndHistory_Type(int status, int type);

	List<DataMoveDetail> findByHistory_Status(int status);

	DataMoveDetail findByHistory_HistoryId(long historyId);

  @Query(value = "select * from data_move_detail d inner join history h on h.history_id=d.history_id where h.crontab is not null or crontab <> ''", nativeQuery = true)
  List<DataMoveDetail> findCrontabTask();

  @Query(value = "SELECT count(1) FROM data_move_detail dm inner join history h on h.history_id=dm.history_id where nt=?1 and h.status = 0 and dm.step =2 and task_id != 0", nativeQuery = true)
  int getRunningDapperTaskByNT(String nt);
}