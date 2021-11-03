package com.ebay.dss.zds.dao;

import com.ebay.dss.zds.model.ZetaTrackLog;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ZetaTrackLogRepository extends JpaRepository<ZetaTrackLog, String> {

  List<ZetaTrackLog> findByEventId(String eventId, Sort sort);
}
