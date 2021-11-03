package com.ebay.dss.zds.service;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.ebay.dss.zds.common.JsonUtil;
import com.ebay.dss.zds.model.ZetaResponse;
import com.ebay.dss.zds.dao.SourceTableRepository;
import com.ebay.dss.zds.exception.ToolSetCheckException;
import com.ebay.dss.zds.model.SourceTableCheck;
import com.ebay.dss.zds.model.SourceTableInfo;

/**
 * Created by zhouhuang on 2018年5月15日
 */
@Service
public class SourceTableCheckService {

	@Autowired
	private JiraService jiraService;

	@Autowired
	private SourceTableRepository sourceTableRepo;

	@Resource(name = "resttemplate")
	private RestTemplate restTemplate;

	@Value("${spring.profiles.active}")
	private String env;

	@Value("${ido.host.url}")
	private String doeServiceHost;

	private static final String TABLE_MATCH_URL = "/api/recon/getTableLastStatus?platform=HERCULES&tablename={tablename}";
	private static final String TABLE_MIGRATE_URL = "/api/asset/getVegaTableStatus?table={table}";
	private static final String TABLE_EXIST_URL = "/api/asset/getTableLocation?platform=hercules&databasename={databasename}&tablename={tablename}";

	public ZetaResponse<Object> dailyCopyAction(SourceTableInfo sourceTableInfo) {
		String jiraRes = "";
		if (env.equalsIgnoreCase("prod")) {
			String request = jiraService.buildJson(sourceTableInfo.getTblName(), sourceTableInfo.getContent());
			jiraRes = jiraService.createTicket(request);
		} else {
			jiraRes = "{\r\n" + " \"id\": \"2717459\",\r\n"
					+ " \"key\":\"VEGA-1733\",\r\n\"self\":\"https://jirap.corp.ebay.com/rest/api/2/issue/2717459\"\r\n"
					+ "}";
		}
		Map<String, String> jiraMap = JsonUtil.fromJson(jiraRes, Map.class);
		if (jiraMap.containsKey("key")) {
			String jiraKey = jiraMap.get("key");
			sourceTableInfo.setJira(jiraKey);
			sourceTableRepo.save(sourceTableInfo);
			return new ZetaResponse<Object>("Send Request Success", HttpStatus.OK);
		} else {
			throw new ToolSetCheckException("Send Request Failed!");
		}

	}

	public List<SourceTableCheck> getSourceTableCheck(List<SourceTableInfo> tableLists) {
		List<SourceTableCheck> res = new LinkedList<>();
		for (SourceTableInfo table : tableLists) {
			SourceTableCheck sourceTableCheck = new SourceTableCheck(table.getDbName(), table.getTblName());
			StringBuilder tblName = new StringBuilder();
			String dbName = table.getDbName();
			// daily copy action
			if (dbName == null || dbName.trim().length() == 0) {
				dbName = "default";
				SourceTableInfo sourceTableInfo = sourceTableRepo.findSourceTableInfoByTblName(table.getTblName());
				Optional.ofNullable(sourceTableInfo).ifPresent(sti->sourceTableCheck.setJira(sti.getJira()));
			} else {
				tblName.append(dbName);
				tblName.append(".");
				SourceTableInfo sourceTableInfo = sourceTableRepo.findSourceTableInfoByDbNameAndTblName(dbName,
						table.getTblName());
				Optional.ofNullable(sourceTableInfo).ifPresent(sti->sourceTableCheck.setJira(sti.getJira()));
			}
			tblName.append(table.getTblName());
			// table match
			List<Object> tableMatch = restTemplate.getForObject(doeServiceHost + TABLE_MATCH_URL, List.class,
					tblName.toString().trim());
			if (tableMatch.size() > 0) {
				Map<String, Object> tableMatchMap = (Map<String, Object>) tableMatch.get(0);
				if ("pass".equalsIgnoreCase((String) tableMatchMap.get("status"))) {
					tableMatchMap.put("status", 1);
				} else if ("fail".equalsIgnoreCase((String) tableMatchMap.get("status"))) {
					tableMatchMap.put("status", 0);
				} else {
					tableMatchMap = null;
				}
				sourceTableCheck.setIsMatch(tableMatchMap);
			}
			// table mirgrate&dailyCopy
			Map<String, Map<String, List<Map<String, Object>>>> tableMigrate = restTemplate
					.getForObject(doeServiceHost + TABLE_MIGRATE_URL, Map.class, table.getTblName().trim());
			Map<String, Object> value = tableMigrate.get("data").get("value").get(0);
			Optional.ofNullable(value.get("migration_flag")).ifPresent(v->sourceTableCheck.setIsMigrate(Integer.parseInt(v.toString())));
			Optional.ofNullable(value.get("data_copy_flag")).ifPresent(v->sourceTableCheck.setIsDailyCopy(Integer.parseInt(v.toString())));
			// table exist
			List<Object> tableExist = restTemplate.getForObject(doeServiceHost + TABLE_EXIST_URL, List.class,
					dbName.trim(), table.getTblName().trim());
			Map<String, Object> isExist = new HashMap<>();
			if (tableExist.size() > 0) {
				isExist.put("status", 1);
				Map<String, String> location = (Map<String, String>) tableExist.get(0);
				isExist.put("hdfs", location.get("location"));
			} else {
				isExist.put("status", 0);
				isExist.put("hdfs", null);
			}
			sourceTableCheck.setIsExist(isExist);
			res.add(sourceTableCheck);

		}
		return res;
	}
}
