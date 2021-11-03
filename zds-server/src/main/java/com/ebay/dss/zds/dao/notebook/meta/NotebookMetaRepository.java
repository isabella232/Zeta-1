package com.ebay.dss.zds.dao.notebook.meta;

import com.ebay.dss.zds.model.notebook.meta.DoeNotebookMetaProjection;
import com.ebay.dss.zds.model.notebook.meta.NotebookMeta;
import com.ebay.dss.zds.model.notebook.ZetaNotebookSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NotebookMetaRepository extends JpaRepository<NotebookMeta, String> {
//    @Query(value = "SELECT nb.title, nb.nt, nb.update_dt, nb.last_run_dt, meta.* FROM zeta_notebook nb " +
//        "INNER JOIN zeta_notebook_meta meta ON meta.id = nb.id " +
//        "WHERE nb.id = ?1", nativeQuery = true)
    NotebookMeta findOneById(String id);

    @Query(value = "SELECT nb.title, meta.id, meta.description, meta.reference, meta.notebook_type FROM zeta_notebook nb " +
        "INNER JOIN zeta_notebook_meta meta ON meta.id = nb.id " +
        "WHERE nb.id = ?1", nativeQuery = true)
    DoeNotebookMetaProjection findDoeMetaById(String id);

    @Query(value = "SELECT nb.title, nb.nt, nb.preference, nb.update_dt as lastUpdateDt, nb.last_run_dt as lastRunDt,\n" +
        "count(distinct req.id) as executedCnt,\n" +
        "count(distinct fv.nt) as favoriteCnt, meta.*\n" +
        "FROM zeta_notebook nb\n" +
        "LEFT JOIN zeta_notebook_meta meta ON meta.id = nb.id\n" +
        "LEFT JOIN zeta_job_request req on nb.id = req.notebook_id\n" +
        "LEFT JOIN zeta_favorite fv on nb.id = fv.id AND fv.favorite_type = 'share_nb' AND fv.favorite = '1'\n" +
        "WHERE nb.id in ?1\n" +
        "GROUP BY nb.id", nativeQuery = true)
    List<ZetaNotebookSummary> findSummaryByIds(List<String> ids);
}
