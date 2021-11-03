package com.ebay.dss.zds.dao;

import com.ebay.dss.zds.model.metatable.ZetaMetaTable;
import com.ebay.dss.zds.model.metatable.ZetaMetaTable.MetaTableStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ZetaMetaTableRepository extends JpaRepository<ZetaMetaTable, String> {

  List<ZetaMetaTable> findByMetaTableStatusNotOrderByUpdateTimeDesc(MetaTableStatus metaTableStatus);

  Optional<ZetaMetaTable> findByIdAndMetaTableStatusNot(String id, MetaTableStatus metaTableStatus);

  Optional<ZetaMetaTable> findByMetaTableNameAndNtAndMetaTableStatusNot
      (String metaTableName, String nt, MetaTableStatus metaTableStatus);

  Optional<ZetaMetaTable> findByDbAndTblAndPlatform(String db, String tbl, String platform);

  @Transactional
  @Modifying
  @Query(value = "update zeta_meta_table set path = ?2 where id = ?1", nativeQuery = true)
  int updateTablePath(String noteId, String path);

  @Transactional
  @Modifying
  @Query(value = "update zeta_meta_table set auth_info=?1 where id = ?2", nativeQuery = true)
  int updateMetaTableAuthInfo(String authInfo, String id);
}
