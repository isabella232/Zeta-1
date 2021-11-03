package com.ebay.dss.zds.rest;

import java.util.List;
import java.util.Map;

import com.ebay.dss.zds.model.History;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.ebay.dss.zds.model.ZetaResponse;
import com.ebay.dss.zds.model.DataValidateDetail;
import com.ebay.dss.zds.service.DataValidationService;

/**
 * Created by zhouhuang on Apr 26, 2018
 */
@RestController
@RequestMapping("/DataValidation")
public class DataValidationController {

	@Autowired
	private DataValidationService dataValidationService;

	@RequestMapping(path = "/validate", method = RequestMethod.POST)
	public ZetaResponse<Object> validate(@RequestBody DataValidateDetail dataValidateDetail) {
		return dataValidationService.save(dataValidateDetail);
	}

	@RequestMapping(path = "/history", method = RequestMethod.GET)
	@ResponseBody
	public List<Map<String, Object>> getHistory(String nt) {
		return dataValidationService.getHistory(nt);
	}

	@RequestMapping(path = "/history/detail", method = RequestMethod.GET)
	@ResponseBody
	public History getHistoryDetail(Long historyId) {
		return dataValidationService.getHistoryDetail(historyId);
	}
}
