package com.ebay.dss.zds.dao;

import com.ebay.dss.zds.model.alation.AlationQueryMeta;
import com.sun.istack.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AlationQueryMetaRepository extends JpaRepository<AlationQueryMeta, Integer> {
  @Query(value = "SELECT meta.* FROM alation_integrate_meta meta " +
      "LEFT JOIN alation_query_map mapping ON meta.alation_query_id = mapping.query_id " +
      "WHERE meta.author = ?1", nativeQuery = true)
  List<AlationQueryMeta> findByAuthor(String author);
}
