package com.ebay.dss.zds.rest;


import com.ebay.dss.zds.model.ZetaResponse;
import com.ebay.dss.zds.model.ZetaWorkspaceInstance;
import com.ebay.dss.zds.rest.annotation.AuthenticationNT;
import com.ebay.dss.zds.service.ZetaWorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/workspace")
public class ZetaWorkspaceController {

  @Autowired
  ZetaWorkspaceService zetaWorkSpaceService;


  @PutMapping("/{id}/{type}/seq/{seq}")
  public ZetaResponse updateNoteSeq(@AuthenticationNT String nt
      , @PathVariable("id") String id
      , @PathVariable("seq") int seq
      , @PathVariable("type") String type) {
    ZetaWorkspaceInstance instance = new ZetaWorkspaceInstance();
    instance.setId(id);
    instance.setNt(nt);
    instance.setSeq(seq);
    instance.setType(type);
    return zetaWorkSpaceService.updateNoteSeq(instance);
  }

  @PutMapping("/seqs")
  public ZetaResponse updateNoteSeq(@AuthenticationNT String nt
      , @RequestBody List<ZetaWorkspaceInstance> wss) {
    wss.forEach(ws -> ws.setNt(nt));
    return zetaWorkSpaceService.updateNotesSeq(wss);
  }

  @GetMapping("/getOpenedNote")
  public List<Map<String, Object>> getOpenedNote(@AuthenticationNT String nt) {
    return zetaWorkSpaceService.getOpendNote(nt);
  }
}
