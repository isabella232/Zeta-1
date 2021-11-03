package com.ebay.dss.zds.model;

import java.util.List;

/**
 * Created by zhouhuang on 2018年7月25日
 */
public class SqlConvertInfo {

	private String platform;
	private String dbName;
	private String tblName;
	private List<String> sqlScripts;
	private List<String> tblInfos;

	/**
	 * @return the platform
	 */
	public String getPlatform() {
		return platform;
	}

	/**
	 * @param platform
	 *            the platform to set
	 */
	public void setPlatform(String platform) {
		this.platform = platform;
	}

	/**
	 * @return the dbName
	 */
	public String getDbName() {
		return dbName;
	}

	/**
	 * @param dbName
	 *            the dbName to set
	 */
	public void setDbName(String dbName) {
		this.dbName = dbName;
	}

	/**
	 * @return the tblName
	 */
	public String getTblName() {
		return tblName;
	}

	/**
	 * @param tblName
	 *            the tblName to set
	 */
	public void setTblName(String tblName) {
		this.tblName = tblName;
	}

	/**
	 * @return the sqlScripts
	 */
	public List<String> getSqlScripts() {
		return sqlScripts;
	}

	/**
	 * @param sqlScripts
	 *            the sqlScripts to set
	 */
	public void setSqlScripts(List<String> sqlScripts) {
		this.sqlScripts = sqlScripts;
	}

	/**
	 * @return the tblInfos
	 */
	public List<String> getTblInfos() {
		return tblInfos;
	}

	/**
	 * @param tblInfos the tblInfos to set
	 */
	public void setTblInfos(List<String> tblInfos) {
		this.tblInfos = tblInfos;
	}

}
