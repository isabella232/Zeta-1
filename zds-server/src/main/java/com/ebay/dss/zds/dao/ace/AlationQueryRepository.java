package com.ebay.dss.zds.dao.ace;

import com.ebay.dss.zds.model.ace.AlationQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AlationQueryRepository extends JpaRepository<AlationQuery, Integer> {

    Optional<AlationQuery> findOneByUrl(String url);

}
