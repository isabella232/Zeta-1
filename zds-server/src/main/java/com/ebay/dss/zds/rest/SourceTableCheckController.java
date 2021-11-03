package com.ebay.dss.zds.rest;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.ebay.dss.zds.model.SourceTableCheck;
import com.ebay.dss.zds.model.SourceTableInfo;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.ebay.dss.zds.model.ZetaResponse;
import com.ebay.dss.zds.service.SourceTableCheckService;

/**
 * Created by zhouhuang on 2018年5月15日
 */

@RestController
@RequestMapping("/SourceTableCheck")
public class SourceTableCheckController {

	@Autowired
	private SourceTableCheckService sourceTableCheckService;

	@RequestMapping(path = "/dailyCopyAction", method = RequestMethod.POST)
	public ZetaResponse<Object> dailyCopyAction(@RequestBody SourceTableInfo sourceTableInfo) {
		return sourceTableCheckService.dailyCopyAction(sourceTableInfo);
	}

	@RequestMapping(path = "/getSourceTableCheck", method = RequestMethod.POST)
	public List<SourceTableCheck> getSourceTableCheck(@RequestBody List<SourceTableInfo> tableLists) {
		return sourceTableCheckService.getSourceTableCheck(tableLists);
	}

}