package com.ebay.dss.zds.dao;

import com.ebay.dss.zds.model.ZetaNotebook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ZetaPublicNotebookRepository extends JpaRepository<ZetaNotebook, String> {

    @Modifying
    @Query("update ZetaNotebook set public_role = 'pub' " +
            "where public_role = 'no_pub' and id = :nid and nt = :nt")
    public int updateNotebookPublic(@Param("nid") String notebookId, @Param("nt") String owner);

    @Query("from ZetaNotebook where public_referred = :nid and nt = :nt")
    public ZetaNotebook findPublicNotebook(@Param("nid") String referredNotebook, @Param("nt") String referNt);

    @Query("from ZetaNotebook where public_role = 'pub'")
    public List<ZetaNotebook> findAllPublicNotebook();
}
