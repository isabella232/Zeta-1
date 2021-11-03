package com.ebay.dss.zds.rest;

import com.ebay.dss.zds.model.ZetaResponse;
import com.ebay.dss.zds.model.authorization.AuthorizationInfo;
import com.ebay.dss.zds.model.authorization.AuthorizationInfo.AuthType;
import com.ebay.dss.zds.model.metatable.MetaTableOperation;
import com.ebay.dss.zds.model.metatable.ZetaMetaTable;
import com.ebay.dss.zds.rest.annotation.AuthenticationNT;
import com.ebay.dss.zds.service.metatable.ZetaMetaTableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/MetaTable")
public class ZetaMetaTableController {

  @Autowired
  ZetaMetaTableService zetaMetaTableService;

  @GetMapping("/list")
  public List<ZetaMetaTable> getZetaMetaTableList(@AuthenticationNT String nt) {
    return zetaMetaTableService.getZetaMetaTableList(nt);
  }

  @PostMapping("/create")
  public ZetaResponse createZetaMetaTable(@AuthenticationNT String nt
      , @RequestBody ZetaMetaTable zetaMetaTable) {
    return zetaMetaTableService.createZetaMetaTable(nt, zetaMetaTable);
  }

  @PutMapping("/update/{id}")
  public ZetaResponse updateZetaMetaTable(@AuthenticationNT String nt
      , @PathVariable("id") String id
      , @RequestBody MetaTableOperation metaTableOperation) {
    return zetaMetaTableService.updateZetaMetaTable(nt, id, metaTableOperation);
  }

  @PutMapping("/updatePath")
  public List<ZetaMetaTable> updatePath(@AuthenticationNT String nt
      , @RequestBody List<ZetaMetaTable> metaTables) {
    zetaMetaTableService.updateZetaMetaTablePath(nt, metaTables);
    return getZetaMetaTableList(nt);
  }

  @GetMapping("/sync/status/{id}")
  public ZetaResponse getSyncStatus(@AuthenticationNT String nt
      , @PathVariable("id") String id) {
    return zetaMetaTableService.getSyncStatus(nt, id);
  }

  @PutMapping("/sync/{id}")
  public ZetaResponse syncHadoopTable(@AuthenticationNT String nt
      , @PathVariable("id") String id
      , @RequestBody ZetaMetaTable zetaMetaTable) {
    return zetaMetaTableService.syncHadoopTable(nt, id, zetaMetaTable);
  }

  @DeleteMapping("/delete/{id}")
  public ZetaResponse deleteZetaMetaTable(@AuthenticationNT String nt
      , @PathVariable("id") String id) {
    return zetaMetaTableService.deleteZetaMetaTable(nt, id);
  }

  @PutMapping("/grant/{id}")
  public ZetaResponse grantAccess(@AuthenticationNT String nt
      , @PathVariable("id") String id
      , @RequestBody Map<AuthType, Set<AuthorizationInfo>> authInfo) {
    return zetaMetaTableService.grantAuthInfo(nt, id, authInfo);
  }

  @GetMapping("/{id}/info")
  public Object getZetaMetaTableInfo(@AuthenticationNT String nt,
                                     @PathVariable("id") String id) {
    return zetaMetaTableService.getZetaMetaTableInfo(nt, id, true);
  }

  @PutMapping("/edit/{id}")
  public ZetaResponse editZetaMetaTable(@AuthenticationNT String nt
      , @PathVariable("id") String id
      , @RequestBody MetaTableOperation metaTableOperation) {
    return zetaMetaTableService.editZetaMetaTable(nt, id, metaTableOperation);
  }

  @GetMapping("/whitelist")
  public Object getZetaMetaTableWhiteList(@AuthenticationNT String nt) {
    return zetaMetaTableService.getZetaMetaTableWhiteList();
  }

  @GetMapping("/share/{id}")
  public Object getZetaMetaTable(@PathVariable("id") String id) {
    return zetaMetaTableService.getZetaMetaTableInfo(null, id, false);
  }

  @PutMapping("/applyAccess/{nt}/{id}")
  public ZetaResponse applyAccess(@PathVariable("nt") String nt
      , @PathVariable("id") String id) {
    return zetaMetaTableService.applyAccess(nt, id);
  }
}
