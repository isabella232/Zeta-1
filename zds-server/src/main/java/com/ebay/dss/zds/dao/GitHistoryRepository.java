package com.ebay.dss.zds.dao;

import com.ebay.dss.zds.model.GitHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Created by zhouhuang on 2019/3/15.
 */
@Repository
public interface GitHistoryRepository extends JpaRepository<GitHistory, Long> {}
