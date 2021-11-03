package com.ebay.dss.zds.dao;


import com.ebay.dss.zds.model.ZetaOperationLog;
import com.ebay.dss.zds.model.ZetaOperationLog.OperationType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ZetaOperationLogRepository extends JpaRepository<ZetaOperationLog, String> {

  List<ZetaOperationLog> findByOperationIdAndOperationType(
      String operationId, OperationType operationType, Sort sort);
}
