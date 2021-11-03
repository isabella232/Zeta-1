package com.ebay.dss.zds.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by zhouhuang on Apr 27, 2018
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SourceTableCheck {

	@JsonProperty
	private String dbName;

	@JsonProperty
	private String tblName;

	@JsonProperty
	private int isMigrate;

	@JsonProperty
	private Map<String, Object> isMatch;

	@JsonProperty
	private Map<String, Object> isExist;

	@JsonProperty
	private int isDailyCopy;

	@JsonProperty
	private String jira;
	
	public SourceTableCheck(){
		
	}
	
	public SourceTableCheck(String dbName, String tblName) {
		this.dbName = dbName;
		this.tblName = tblName;
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
	 * @return the isMigrate
	 */
	public int getIsMigrate() {
		return isMigrate;
	}

	/**
	 * @param isMigrate
	 *            the isMigrate to set
	 */
	public void setIsMigrate(int isMigrate) {
		this.isMigrate = isMigrate;
	}

	/**
	 * @return the isMatch
	 */
	public Map<String, Object> getIsMatch() {
		return isMatch;
	}

	/**
	 * @param isMatch
	 *            the isMatch to set
	 */
	public void setIsMatch(Map<String, Object> isMatch) {
		this.isMatch = isMatch;
	}

	/**
	 * @return the isExist
	 */
	public Map<String, Object> getIsExist() {
		return isExist;
	}

	/**
	 * @param isExist
	 *            the isExist to set
	 */
	public void setIsExist(Map<String, Object> isExist) {
		this.isExist = isExist;
	}

	/**
	 * @return the isDailyCopy
	 */
	public int getIsDailyCopy() {
		return isDailyCopy;
	}

	/**
	 * @param isDailyCopy
	 *            the isDailyCopy to set
	 */
	public void setIsDailyCopy(int isDailyCopy) {
		this.isDailyCopy = isDailyCopy;
	}

	/**
	 * @return the jira
	 */
	public String getJira() {
		return jira;
	}

	/**
	 * @param jira
	 *            the jira to set
	 */
	public void setJira(String jira) {
		this.jira = jira;
	}

}
