package com.ebay.dss.zds.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;

/**
 * Created by zhouhuang on May 3, 2018
 */
@Service
public class MetaDataService {

	@Resource(name = "resttemplate")
	private RestTemplate restTemplate;

	public Object findTableDtl(String platform, String dbName, String tblName) {
		return restTemplate.getForObject(
				"http://zeta.corp.ebay.com/zeta-ext-metadata-manager-0.0.1-SNAPSHOT/metadata/findTableDtl?platform={platform}&dbName={dbName}&tblName={tblName}",
				Object.class, platform.trim(), dbName.trim(), tblName.trim());
	}

	public Object findFDTWithSQLScripts(String platform, String dbName, String tblName) {
		return restTemplate.getForObject(
				"http://zeta.corp.ebay.com/zeta-ext-metadata-manager-0.0.1-SNAPSHOT/metadata/findFDTWithSQLScripts?platform={platform}&dbName={dbName}&tblName={tblName}",
				Object.class, platform.trim(), dbName.trim(), tblName.trim());
	}

}
