package com.ebay.dss.zds.dao;

import com.ebay.dss.zds.model.ZetaCache;
import org.springframework.data.repository.CrudRepository;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

@Repository
public interface ZetaCacheRepository extends CrudRepository<ZetaCache, String> {
    @Nullable
    ZetaCache findByKeyName(String key);
}
