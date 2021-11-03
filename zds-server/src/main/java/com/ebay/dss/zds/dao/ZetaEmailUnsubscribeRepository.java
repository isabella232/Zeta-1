package com.ebay.dss.zds.dao;

import com.ebay.dss.zds.model.ZetaEmailUnsubscribe;
import org.springframework.data.repository.CrudRepository;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Deprecated
public interface ZetaEmailUnsubscribeRepository extends CrudRepository<ZetaEmailUnsubscribe, String> {
    @Nullable
    ZetaEmailUnsubscribe findByUnsubscribeId(String jobId);

    List<ZetaEmailUnsubscribe> findByNt(String nt);
}
