package com.ebay.dss.zds.dao;

import com.ebay.dss.zds.model.ReleaseHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by zhouhuang on 2019/3/15.
 */
@Repository
public interface ReleaseHistoryRepository extends JpaRepository<ReleaseHistory,Long>{

    @Query(value = "SELECT * from release_history where nt = ?1 and create_time >= ?2 ;", nativeQuery = true)
    List<ReleaseHistory> findCurrentHistory(String nt,String currentDay);

    Long countByReleaseTag(String tag);
}
