package com.ebay.dss.zds.rest;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.ebay.dss.zds.rest.annotation.AuthenticationNT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import com.ebay.dss.zds.model.Request;
import com.ebay.dss.zds.model.SqlConvertInfo;
import com.ebay.dss.zds.service.SQLConvertService;

/**
 * Created by zhouhuang on 2018年5月15日
 */
@RestController
@RequestMapping("/SQLConvert")
public class SQLConvertController {
	@Autowired
	private SQLConvertService sqlConvertService;

	@RequestMapping(path = "/convertsql", method = RequestMethod.POST)
	public List<Map<String, String>> convertSQL(@RequestBody List<String> sqlScripts) {
		Map<String, List<Map<String, String>>> convertSQL = sqlConvertService.convertSQL(sqlScripts);
		return Optional.ofNullable(convertSQL.get("Success")).orElseGet(() -> convertSQL.get("Fail"));
	}

    @RequestMapping(path = "/convert", method = RequestMethod.POST)
    public Map<String, List<Map<String, String>>> convert(@AuthenticationNT String nt, @RequestBody
            SqlConvertInfo sqlConvertInfo) {
        return sqlConvertService.convert(sqlConvertInfo, nt);
    }

    @RequestMapping(path = "/manualconvertsql", method = RequestMethod.POST)
    public Object manualConvertSQL(@AuthenticationNT String nt, @RequestBody Request requestInfo) {
        return sqlConvertService.manualConvertSQL(requestInfo, nt);
    }

	@RequestMapping(path = "/getDDL", method = RequestMethod.POST)
	public List<Map<String, String>> getDDLs(@RequestBody List<String> tblInfos) {
		Map<String, List<Map<String, String>>> getDDL = sqlConvertService.getDDL(tblInfos);
		return Optional.ofNullable(getDDL.get("Success")).orElseGet(() -> getDDL.get("Fail"));
	}

	@RequestMapping(path = "/getTdTableSource", method = RequestMethod.POST)
	public Map<String, List<String>> getTdTableSource(@RequestBody List<String> sqlScripts) {
		return sqlConvertService.getTdTableSource(sqlScripts);
	}

	@RequestMapping(path = "/getSparkTableSource", method = RequestMethod.POST)
	public Object getSparkTableSource(@RequestBody Request request) {
		return sqlConvertService.getSparkTableSource(request);
	}

	@RequestMapping(path = "/saveToNotebook", method = RequestMethod.POST)
	public Map<String, List<Map<String, String>>> saveToNotebook(@RequestBody Map<String, Object> files) {
		return sqlConvertService.saveToNotebook(files);
	}

//	@RequestMapping(value = "/downloadSQL", method = RequestMethod.POST)
//	public ResponseEntity<byte[]> downloadSQL(HttpServletRequest request, HttpServletResponse response)
//			throws Exception {
//		File zipFile = null;
//		try {
//			Request vo = JsonUtil.fromJson(request.getParameter("fileName"), Request.class);
//			zipFile = sqlConvertService.download(vo, "SQL");
//			HttpHeaders headers = new HttpHeaders();
//			headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
//			headers.setContentDispositionFormData("attachment", vo.getTableName() + "_SQL.zip");
//			return new ResponseEntity<byte[]>(FileUtils.readFileToByteArray(zipFile), headers, HttpStatus.CREATED);
//		} catch (IOException e) {
//			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//		} finally {
//			if (zipFile != null)
//				zipFile.delete();
//		}
//		return null;
//	}

//	@RequestMapping(value = "/downloadDDL", method = RequestMethod.POST)
//	public ResponseEntity<byte[]> downloadDDL(HttpServletRequest request, HttpServletResponse response)
//			throws Exception {
//		File zipFile = null;
//		try {
//			Request vo = JsonUtil.fromJson(request.getParameter("fileName"), Request.class);
//			zipFile = sqlConvertService.download(vo, "DDL");
//			HttpHeaders headers = new HttpHeaders();
//			headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
//			headers.setContentDispositionFormData("attachment", vo.getTableName() + "_DDL.zip");
//			return new ResponseEntity<byte[]>(FileUtils.readFileToByteArray(zipFile), headers, HttpStatus.CREATED);
//		} catch (IOException e) {
//			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//		} finally {
//			if (zipFile != null)
//				zipFile.delete();
//		}
//		return null;
//	}
}
