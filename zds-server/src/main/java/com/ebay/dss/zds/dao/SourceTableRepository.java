package com.ebay.dss.zds.dao;


import org.springframework.data.jpa.repository.JpaRepository;

import com.ebay.dss.zds.model.SourceTableInfo;
import org.springframework.stereotype.Repository;


/**
 * Created by zhouhuang on Apr 26, 2018
 */
@Repository
public interface SourceTableRepository extends JpaRepository<SourceTableInfo, Long> {
	
	SourceTableInfo findSourceTableInfoByDbNameAndTblName(String dbName,String tblName);
	
	SourceTableInfo findSourceTableInfoByTblName(String tblName);

}
