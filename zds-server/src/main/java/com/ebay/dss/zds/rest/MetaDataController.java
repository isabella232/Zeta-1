package com.ebay.dss.zds.rest;

import com.ebay.dss.zds.service.MetaDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;


/**
 * Created by zhouhuang on May 3, 2018
 */
@RestController
@RequestMapping("/metadata")
public class MetaDataController {

	@Autowired
	private MetaDataService metadataService;

	@RequestMapping(path = "/findTableDtl", method = RequestMethod.GET)
	@ResponseBody
	public Object findTableDtl(String platform, String dbName, String tblName) {
		return metadataService.findTableDtl(platform, dbName, tblName);
	}
	
	@RequestMapping(path = "/findFDTWithSQLScripts", method = RequestMethod.GET)
	@ResponseBody
	public Object findFDTWithSQLScripts(String platform, String dbName, String tblName) {
		return metadataService.findFDTWithSQLScripts(platform, dbName, tblName);
	}

}
