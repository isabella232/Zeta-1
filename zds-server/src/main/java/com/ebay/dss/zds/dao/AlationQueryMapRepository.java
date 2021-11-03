package com.ebay.dss.zds.dao;

import com.ebay.dss.zds.model.alation.AlationQueryMap;
import com.ebay.dss.zds.model.alation.AlationQueryMapPK;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AlationQueryMapRepository extends JpaRepository<AlationQueryMap, AlationQueryMapPK> {
    @Query(value = "select map.* from alation_query_map map \n" +
        "inner join zeta_notebook note on map.notebook_id = note.id\n" +
        "where map.query_id = ?1 and note.nt = ?2 limit 1", nativeQuery = true)
    AlationQueryMap findByQueryIdAndAuthor(int queryId, String nt);
}