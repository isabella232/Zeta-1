package com.ebay.dss.zds.service;
/** 
* Created by zhouhuang on Sep 14, 2016 
*/

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.ebay.dss.zds.common.DateUtil;

@Service
public class JiraService {

	@Resource(name = "resttemplate")
	private RestTemplate restTemplate;

	public String buildJson(String tblName, String content) {
		String jsonFormat = "{\"fields\":{\"summary\":\"[Test Pls Ignore]Dependency Table Landing - <%s>\",\"issuetype\":{\"name\":\"User Story\"},\"customfield_15302\":{\"value\":\"Not Triaged\"},\"customfield_14400\":\"VEGA-13\",\"labels\":[\"zeta-dev-suite\"],\"priority\":{\"name\":\"P4\"},\"project\":{\"key\":\"VEGA\"},\"assignee\":{\"name\":\"weiywang\"},\"components\":[{\"name\":\"Trust Batch Migration\"}],\"description\":\"%s\",\"duedate\":\"%s\"}}";
		return String.format(jsonFormat, tblName, content, DateUtil.addDay(14, DateUtil.DT));
	}

	public String createTicket(String request) {
		return restTemplate.postForObject("http://m1.corp.ebay.com/api/jira/createTicket", request, String.class);
	}

	public String searchTickets(String request) {
		return restTemplate.postForObject("http://m1.corp.ebay.com/api/jira/searchTickets", request, String.class);
	}

	public String updateTicket(String request) {
		return restTemplate.postForObject("http://m1.corp.ebay.com/api/jira/updateTicket", request, String.class);
	}

	public boolean checkUserAssignable(String assignee, String project) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("project", project);
		params.put("username", assignee);
		String res = restTemplate.getForObject("http://m1.corp.ebay.com/api/jira/checkUserAssignable", String.class,
				params);
		if ("null".equalsIgnoreCase(res)) {
			return false;
		} else {
			return true;
		}
	}
}
