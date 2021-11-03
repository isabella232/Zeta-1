package com.ebay.dss.zds.service;


import com.ebay.dss.zds.dao.ZetaWorkspaceRepository;
import com.ebay.dss.zds.model.ZetaResponse;
import com.ebay.dss.zds.model.ZetaWorkspaceInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ZetaWorkspaceService {

  @Autowired
  ZetaWorkspaceRepository zetaWorkSpaceRepository;


  public ZetaResponse updateNotesSeq(List<ZetaWorkspaceInstance> instances) {
    zetaWorkSpaceRepository.saveAll(instances);
    return new ZetaResponse<>("success", HttpStatus.OK);
  }

  public ZetaResponse updateNoteSeq(ZetaWorkspaceInstance instance) {
    zetaWorkSpaceRepository.save(instance);
    return new ZetaResponse<>("success", HttpStatus.OK);
  }

  public List<Map<String, Object>> getOpendNote(String nt) {
    return zetaWorkSpaceRepository.findOpenedByNt(nt);
  }
}
