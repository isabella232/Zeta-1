package com.ebay.dss.zds.dao.ace;

import com.ebay.dss.zds.model.ace.AceEnotifyRead;
import com.ebay.dss.zds.model.ace.AceEnotifyReadId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AceEnotifyReadRepository extends JpaRepository<AceEnotifyRead, AceEnotifyReadId> {

    List<AceEnotifyRead> findAllByEnotifyIdInAndNt(Iterable<Long> ids, String nt);
}
