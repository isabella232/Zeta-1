package com.ebay.dss.zds.service;

import com.ebay.dss.zds.auth.impl.jwt.JwtAuthenticationService;
import com.ebay.dss.zds.common.FileHelper;
import com.ebay.dss.zds.common.JsonUtil;
import com.ebay.dss.zds.dao.ZetaNotebookRepository;
import com.ebay.dss.zds.model.Request;
import com.ebay.dss.zds.model.SqlConvertInfo;
import com.ebay.dss.zds.model.ZetaNotebook;
import com.ebay.dss.zds.message.EventTracker;
import com.ebay.dss.zds.message.event.Event;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

/**
 * Created by zhouhuang on 2018年5月15日
 */
@Service
public class SQLConvertService {
    private RestTemplate restTemplate;

    private JwtAuthenticationService jwtAuthenticationService;

    private ZetaNotebookRepository zetaNotebookRepository;


    public SQLConvertService() {
    }

    @Autowired
    public SQLConvertService(@Qualifier("resttemplate") RestTemplate restTemplate,
                             JwtAuthenticationService jwtAuthenticationService,
                             ZetaNotebookRepository zetaNotebookRepository) {
        this.restTemplate = restTemplate;
        this.jwtAuthenticationService = jwtAuthenticationService;
        this.zetaNotebookRepository = zetaNotebookRepository;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(SQLConvertService.class);
    private static final String DW_SQL = "/dw/etl/home/dev/sql";
    private static final String AUTO_CODE = "/dw/etl/home/dev/codes";
    private static final String RElEASE_SQL = "/dw/etl/home/dev/lib/ADPO/release/";
    private static final String RELEASE_DDL = "/dw/etl/home/dev/lib/ADPO/releaseddl/";
//    private static final String CONVERT_SQL = "http://zeta.corp.ebay.com/zeta-sql/api/convertSql";
//    private static final String CONVERT_SQL = "http://zeta-suite-1-1657523.lvs02.eaz.ebayc3.com/sql/v2/convertSql";
//    private static final String CONVERT_SQL = "http://zeta-suite-1-1657523.lvs02.eaz.ebayc3.com/zeta-sql-forvdm/sql/convertSql";

    @Value("${sql.convert.service.url}")
    private String CONVERT_SQL;

    private static final String GET_DDL = "http://zeta.corp.ebay.com/zeta-sql/api/getDDL";
    private static final String GET_TD_TABLE_SOURCE = "http://zeta.corp.ebay.com/zeta-sql/api/getTableSource";
    private static final String GET_SPARK_TABLE_SOURCE = "http://zeta.corp.ebay.com/zeta-sql/api/getSparkTableSource";

    public Map<String, List<Map<String, String>>> convertSQL(List<String> sqlScripts) {
        List<Map<String, String>> res = new LinkedList<>();
        String resultStatus = "Success";
        for (String sqlScript : sqlScripts) {
            String folder = sqlScript.substring(0, sqlScript.indexOf("."));
            String sqlPath = DW_SQL + File.separator + folder + File.separator + sqlScript;
            String sparkSqlFileName = sqlScript.substring(0, sqlScript.lastIndexOf(".sql")) + ".spark.sql";
            String releaseSparkSqlPath = RElEASE_SQL + folder + File.separator;
            String sourceSql = readFile(sqlPath);
            String releaseSql;
            Request request = new Request();
            request.setSql(sourceSql);
            try {
                Map ret = restTemplate.postForObject(CONVERT_SQL, request, Map.class);
                if (ret.get("convertSql") == null) {
                    releaseSql = (String) ret.get("errorMessage");
                } else {
                    releaseSql = (String) ret.get("convertSql");
                }
            } catch (Exception e) {
                resultStatus = "Fail";
                releaseSql = e.getMessage();
            }
            writeFile(releaseSql, releaseSparkSqlPath, sparkSqlFileName);
            Map<String, String> resMap = new HashMap<>();
            resMap.put("filePath", releaseSparkSqlPath);
            resMap.put("fileName", sparkSqlFileName);
            res.add(resMap);
        }
        return ImmutableMap.of(resultStatus, res);
    }

    String readFile(String path) {
        try {
            return FileHelper.readFile(path);
        } catch (FileNotFoundException e) {
            LOGGER.error("read file error", e);
            return "";
        }
    }

    void writeFile(String content, String path, String filename) {
        try {
            FileHelper.writeFile(content, path, filename);
        } catch (IOException e) {
            LOGGER.error("write file error", e);
        }
    }

    public Object manualConvertSQL(Request request, String nT) {
        LOGGER.info("Start to Convert SQL for SQL {}", request.getSql());
        /** change to log
         SQLConvertHistory sqlConvertHistory = getSQLConvertHistory(nT);
         sqlConvertHistory.setSqlInfo(request.getSql());
         sqlConvertHistory.setType(1);
         sqlConvertHistoryRepository.save(sqlConvertHistory);
         */
        try {
            HttpEntity<Object> requestBody = new HttpEntity<>(request);
            Map<String, Object> response = restTemplate.exchange(CONVERT_SQL, HttpMethod.POST, requestBody, Map.class).getBody();
            if (response.containsKey("errorMessage") && Objects.nonNull(response.get("errorMessage"))) {
                EventTracker.postEvent(Event.ZetaManualSQLConvertFailedEvent(nT, request.getSql(), response.get("errorMessage").toString()));
            } else {
                EventTracker.postEvent(Event.ZetaManualSQLConvertSuccessEvent(nT, request.getSql()));
            }
            return response;
        } catch (HttpStatusCodeException e) {
            EventTracker.postEvent(Event.ZetaManualSQLConvertFailedEvent(nT, request.getSql(), e.getMessage()));
            return ResponseEntity.status(e.getRawStatusCode()).headers(e.getResponseHeaders()).body(e
                    .getResponseBodyAsString());
        }
    }

    public Map<String, List<String>> getTdTableSource(List<String> sqlScripts) {
        Map<String, List<String>> res = new HashMap<>();
        for (String sqlScript : sqlScripts) {
            try {
                String folder = sqlScript.substring(0, sqlScript.indexOf("."));
                String sqlPath = DW_SQL + File.separator + folder + File.separator + sqlScript;
                String sourceSql = readFile(sqlPath);
                Request request = new Request();
                request.setSql(sourceSql);
                LOGGER.info("Get Td Table Source Request: {}", JsonUtil.toJson(request));
                List<String> ret = restTemplate.postForObject(GET_TD_TABLE_SOURCE, request, List.class);
                res.put(sqlScript, ret);
            } catch (Exception e) {
                LOGGER.error("Get TD Table Source Failed!", e);
                res.put(sqlScript, null);
            }
        }
        return res;
    }

    public Map<String, List<Map<String, String>>> getDDL(List<String> tblInfos) {
        List<Map<String, String>> res = new LinkedList<>();
        String resultStatus = "Success";
        for (String table : tblInfos) {
            if (table.startsWith("batch_views") || table.startsWith("access_views"))
                continue;
            String sparkDDLFileName = table + ".ddl";
            String folder = table.substring(0, table.indexOf("."));
            String releaseSparkDDLPath = RELEASE_DDL + folder + File.separator;
            Request request = new Request();
            request.setTableName(table);
            String releaseDDL;
            try {
                releaseDDL = (String) restTemplate.postForObject(GET_DDL, request, Object.class);
            } catch (Exception e) {
                resultStatus = "Fail";
                releaseDDL = e.getMessage();
            }
            writeFile(releaseDDL, releaseSparkDDLPath, sparkDDLFileName);
            Map<String, String> resMap = new HashMap<>();
            resMap.put("filePath", releaseSparkDDLPath);
            resMap.put("fileName", sparkDDLFileName);
            res.add(resMap);
        }
        return ImmutableMap.of(resultStatus, res);
    }

//	public File download(Request request, String type) throws Exception {
//		List<String> paths = null;
//		if ("DDL".equals(type)) {
//			paths = request.getTableLists();
//		} else if ("SQL".equals(type)) {
//			paths = request.getSqlLists();
//		}
//		File zipFile = new File("/tmp/" + type + System.currentTimeMillis() + ".zip");
//		ZipOutputStream zipOut = null;
//		try {
//			zipOut = new ZipOutputStream(new FileOutputStream(zipFile));
//			for (String path : paths) {
//				File file = new File(path);
//				FileInputStream input = new FileInputStream(file);
//				zipOut.putNextEntry(new ZipEntry(file.getName()));
//				int temp = 0;
//				while ((temp = input.read()) != -1) {
//					zipOut.write(temp);
//				}
//				zipOut.closeEntry();
//				input.close();
//			}
//		} finally {
//			zipOut.close();
//		}
//		return zipFile;
//	}

    public Object getSparkTableSource(Request request) {

        String query = request.getSql();
        query = query.replaceAll("(/\\*{1,2}[\\s\\S]*?\\*/)|(--.*[^\\n]*)|(#.*[^\\n]*)", "");
        request.setSql(query);
        return restTemplate.postForObject(GET_SPARK_TABLE_SOURCE, request, Object.class);
    }

    public Map<String, List<Map<String, String>>> saveToNotebook(Map<String, Object> files) {
        String nt = files.get("nt").toString();
        String tableName = files.get("tableName").toString();
        Map<String, List<Map<String, String>>> fileLists = (Map<String, List<Map<String, String>>>) files
                .get("fileLists");
        Map<String, List<Map<String, String>>> res = new HashMap<>();
        for (Entry<String, List<Map<String, String>>> fileList : fileLists.entrySet()) {
            String type = fileList.getKey();
            List<Map<String, String>> saveResults = new LinkedList<>();
            for (Map<String, String> file : fileList.getValue()) {
                Map<String, String> saveResult = new HashMap<>();
                saveResult.putAll(file);
                String filePath = file.get("filePath");
                String fileName = file.get("fileName");
                try {
                    zetaNotebookRepository.getNotebookByNtAndTitle(nt, fileName);
                    saveResult.put("status", "Fail");
                    saveResult.put("errorMessage", "File exist in Repository!");
                } catch (EmptyResultDataAccessException e) {
                    ZetaNotebook book = new ZetaNotebook();
                    String content = readFile(filePath + fileName);
                    book.setNt(nt);
                    book.setTitle(fileName);
                    book.setContent(content);
                    book.setNbType(ZetaNotebook.NotebookType.single);
                    book.setPath("sql".equals(type) ? File.separator + tableName + File.separator
                            : File.separator + tableName + File.separator + type + File.separator);
                    try {
                        zetaNotebookRepository.addNotebook(book);
                        saveResult.put("status", "Success");
                    } catch (Exception ex) {
                        saveResult.put("status", "Fail");
                        saveResult.put("errorMessage", ex.getMessage());
                    }
                }
                saveResults.add(saveResult);
            }
            res.put(type, saveResults);
        }

        return res;
    }

    List<Map<String, String>> getFileLists(ADPOTYPE type, SqlConvertInfo info) {
        List<Map<String, String>> files = new LinkedList<>();
        FileHelper.getFileList(AUTO_CODE + File.separator + info.getDbName().trim() + File.separator
                + info.getTblName().trim().toLowerCase() + File.separator + type.name().toLowerCase()
                + File.separator, files);
        return files;
    }

    public Map<String, List<Map<String, String>>> convert(SqlConvertInfo sqlConvertInfo, String nT) {
        LOGGER.info("Start to Convert SQL for Table {}", sqlConvertInfo.getTblName());
        Map<String, List<Map<String, String>>> res = new LinkedHashMap<>();
        try {
            Map<String, List<Map<String, String>>> convertSQL = convertSQL(sqlConvertInfo.getSqlScripts());
            Map<String, List<Map<String, String>>> getDDL = getDDL(sqlConvertInfo.getTblInfos());
            res.put("sql", Optional.ofNullable(convertSQL.get("Success")).orElseGet(() -> convertSQL.get("Fail")));
            res.put("ddl", Optional.ofNullable(getDDL.get("Success")).orElseGet(() -> getDDL.get("Fail")));
            for (ADPOTYPE type : ADPOTYPE.values()) {
                List<Map<String, String>> files = getFileLists(type, sqlConvertInfo);
                res.put(type.name().toLowerCase(), files);
            }
            /** change to log
             SQLConvertHistory sqlConvertHistory = getSQLConvertHistory(nT);
             sqlConvertHistory.setSqlInfo(sqlConvertInfo.getTblName());
             sqlConvertHistory.setType(0);
             sqlConvertHistoryRepository.save(sqlConvertHistory);
             */
            if (convertSQL.containsKey("Fail") || getDDL.containsKey("Fail")) {
                EventTracker.postEvent(Event.ZetaBatchSQLConvertFailedEvent(nT, sqlConvertInfo.getPlatform(), sqlConvertInfo.getDbName() + "." + sqlConvertInfo.getTblName()));
            } else {
                EventTracker.postEvent(Event.ZetaBatchSQLConvertSuccessEvent(nT, sqlConvertInfo.getPlatform(), sqlConvertInfo.getDbName() + "." + sqlConvertInfo.getTblName()));
            }
            return res;
        } catch (Exception e) {
            EventTracker.postEvent(Event.ZetaBatchSQLConvertFailedEvent(nT, sqlConvertInfo.getPlatform(), sqlConvertInfo.getDbName() + "." + sqlConvertInfo.getTblName()));
            throw e;
        }

    }

    /*
    private SQLConvertHistory getSQLConvertHistory(String nT) {
        SQLConvertHistory sqlConvertHistory = new SQLConvertHistory();
        sqlConvertHistory.setCreateDate(new Date());
        sqlConvertHistory.setnT(nT);
        return sqlConvertHistory;
    }
    */

    enum ADPOTYPE {
        uc4, dml, cfg
    }

}
