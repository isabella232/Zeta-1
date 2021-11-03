package com.ebay.dss.zds.dao;

import com.ebay.dss.zds.model.ZetaWorkspaceInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface ZetaWorkspaceRepository extends JpaRepository<ZetaWorkspaceInstance, ZetaWorkspaceInstance.ZetaWorkspaceInstancePK> {
  @Query(value = "(select *, null as name from  zeta_workspace_instance where nt = ?1 and seq >=0 and type <> 'SHARED_NOTEBOOK')\n" +
      "union (select ws.*, nb.title as name from  zeta_workspace_instance ws inner join zeta_notebook nb on ws.id = nb.id where ws.nt = ?1 and ws.seq >=0 and ws.type = 'SHARED_NOTEBOOK')", nativeQuery = true)
  List<Map<String, Object>> findOpenedByNt(String nt);
}
