package com.ebay.dss.zds.rest;

import com.ebay.dss.zds.dao.ZetaNotebookRepository;
import com.ebay.dss.zds.message.EventTracker;
import com.ebay.dss.zds.model.ZetaNotebook;
import com.ebay.dss.zds.model.ZetaNotebookPreference;
import com.ebay.dss.zds.model.ZetaResponse;
import com.ebay.dss.zds.model.notebook.meta.NotebookMeta;
import com.ebay.dss.zds.service.DoeESService;
import com.ebay.dss.zds.service.ZetaNotebookMetaService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ebay.dss.zds.common.Metric2Report.NOTEBOOK_PREFERENCE_APPLY_COUNT;

/**
 * Created by tatian on 2019-11-28.
 */
@Aspect
@Component
public class ZetaNotebookControllerAspect {

  private static final Logger logger = LogManager.getLogger(ZetaNotebookControllerAspect.class);

  @Autowired
  DoeESService doeESService;

  @Autowired
  ZetaNotebookMetaService zetaNotebookMetaService;

  @Autowired
  private ZetaNotebookRepository zetaNotebookRepository;

  @Pointcut("target(com.ebay.dss.zds.rest.ZetaNotebookController)")
  private void inZetaNotebookController() {
  }

  @Pointcut("execution(public * updateNotebookPreferenceById(..))")
  private void updateNotebookPreferenceById() {
  }

  @Around("inZetaNotebookController() && updateNotebookPreferenceById() && args(nt, id, preference)")
  public ZetaResponse reportOnPreferenceChange(
          final ProceedingJoinPoint pjp,
          final String nt,
          final String id,
          final String preference) throws Throwable {
    ZetaResponse res;
    Map<String, String> tags = new HashMap<>();
    try {
      res = (ZetaResponse) pjp.proceed();
      tags.put("status", "success");
      return res;
    } catch (Throwable throwable) {
      tags.put("status", "failure");
      throw throwable;
    } finally {
      tags.put("username", nt);
      tags.put("notebookId", id);
      extractAndPutDetails(tags, preference);
      // make sure this is async && no any side effect
    }
  }

  private void extractAndPutDetails(Map<String, String> tags, String preference) {
    try {
      ZetaNotebookPreference znp = ZetaNotebookPreference.fromJson(preference);
      if (znp.notebookConnection != null) {
        Map<String, Object> connectionMap = znp.notebookConnection;
        for (String key : connectionMap.keySet()) {
          tags.put(key, connectionMap.get(key).toString());
        }
//        tags.put("alias", znp.notebookConnection.alias);
//        tags.put("source", znp.notebookConnection.source);
//        tags.put("clusterId", znp.notebookConnection.clusterId);
//        tags.put("batchAccount", znp.notebookConnection.batchAccount);
//        tags.put("codeType", znp.notebookConnection.codeType);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

//  @Pointcut("execution(public * updateNotebookMeta(..))")
//  private void updateNotebookMeta() {
//
//  }
//
//  @AfterReturning(pointcut = "inZetaNotebookController() &&  updateNotebookMeta() && args(nt, meta)", returning = "res")
//  public void afterUpdateNotebookMeta(final String nt, final NotebookMeta meta, final ZetaResponse<NotebookMeta> res) {
//    if (!res.getStatusCode().equals(HttpStatus.OK)) {
//      return;
//    }
//    if (meta.getIsPublic() == null || !meta.getIsPublic().equals("1")) {
//      doeESService.deleteDOEMeta(meta.getId());
//    }
//    else {
//      buildReference(meta.getId());
//      doeESService.updateDOEMeta(meta.getId());
//    }
//  }
//
//  @Pointcut("execution(public * moveNotebook(..))")
//  private void moveNotebook(){
//
//  }
//
//  @AfterReturning(pointcut = "inZetaNotebookController() &&  moveNotebook() && args(nt, notebook)", returning = "res")
//  public void afterMoveNotebook(final String nt, final ZetaNotebook notebook, final ZetaResponse<ZetaNotebook> res) {
//    if (!res.getStatusCode().equals(HttpStatus.OK)) {
//      return;
//    }
//    if (zetaNotebookMetaService.hasMeta(notebook.getId())) {
//      // won't update meta's reference here
//      doeESService.updateDOEMeta(notebook.getId());
//    }
//  }
//
//  @Pointcut("execution(public * updateNotebook(..))")
//  private void updateNotebook() {
//
//  }
//
//  @AfterReturning(pointcut = "inZetaNotebookController() &&  updateNotebook() && args(nt, notebook)", returning = "res")
//  public void afterUpdateNotebook(final String nt, final ZetaNotebook notebook, final ZetaResponse<ZetaNotebook> res) {
//    if (notebook.getContent() == null) {
//      return;
//    }
//    if (!res.getStatusCode().equals(HttpStatus.OK)) {
//      return;
//    }
//
//    ZetaNotebook resNotebook = res.getBody();
//    if (zetaNotebookMetaService.hasMeta(resNotebook.getId())) {
//      NotebookMeta meta = buildReference(resNotebook);
//      if (meta.getIsPublic() != null && meta.getIsPublic().equals("1")) {
//        doeESService.updateDOEMeta(meta.getId());
//      }
//
//    }
//
//  }
//
//  @Pointcut("execution(public * deleteFolder(..))")
//  private void deleteFolder() {
//
//  }
//
//  @AfterReturning(pointcut = "inZetaNotebookController() &&  deleteFolder()", returning = "res")
//  public void afterDeleteFolder(final ZetaResponse<List<String>> res) {
//    if (!res.getStatusCode().equals(HttpStatus.OK)) {
//      return;
//    }
//    if (res.getBody() != null) {
//      zetaNotebookMetaService.batchDeleteNotebookMeta(res.getBody());
//      doeESService.batchDeleteDOEMeta(res.getBody());
//    }
//  }
//
//  @Pointcut("execution(public * deleteNotebook(..))")
//  private void deleteNotebook() {
//
//  }
//
//  @AfterReturning(pointcut = "inZetaNotebookController() &&  deleteNotebook() && args(nt, id)", returning = "res")
//  public void afterDeleteNotebook(final String nt, final String id, final ZetaResponse<List<String>> res) {
//    if (!res.getStatusCode().equals(HttpStatus.OK)) {
//      return;
//    }
//    if (!zetaNotebookMetaService.hasMeta(id)) {
//      return;
//    }
//    zetaNotebookMetaService.deleteNotebookMeta(id);
//    doeESService.deleteDOEMeta(id);
//
//  }
//
//
//  private NotebookMeta buildReference(String id) {
//    ZetaNotebook notebook = zetaNotebookRepository.getNotebook(id);
//    return buildReference(notebook);
//  }
//
//  private NotebookMeta buildReference(ZetaNotebook notebook) {
//    if (!zetaNotebookMetaService.hasMeta(notebook.getId())) {
//      return null;
//    }
//    String refs = zetaNotebookMetaService.parseNotebookReference(notebook);
//    NotebookMeta meta = zetaNotebookMetaService.getNotebookMetaById(notebook.getId());
//    meta.setReference(refs);
//    return meta;
//  }
}
