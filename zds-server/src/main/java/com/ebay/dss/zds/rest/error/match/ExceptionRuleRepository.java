package com.ebay.dss.zds.rest.error.match;

import com.ebay.dss.zds.model.ExceptionRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExceptionRuleRepository extends JpaRepository<ExceptionRule, Integer> {
}
