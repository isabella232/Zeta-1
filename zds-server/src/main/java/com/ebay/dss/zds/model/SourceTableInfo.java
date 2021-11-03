package com.ebay.dss.zds.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;

/**
 * Created by zhouhuang on Apr 27, 2018
 */
@Entity
@Table(name = "source_table_info")
public class SourceTableInfo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(name = "db_name")
	private String dbName;

	@NotBlank(message = "Table Name Can't Be Empty")
	@Column(name = "tbl_name")
	private String tblName;

	private String jira;

	private String content;

	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(Long id) {
		this.id = id;
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

	/**
	 * @return the content
	 */
	public String getContent() {
		return content;
	}

	/**
	 * @param content
	 *            the content to set
	 */
	public void setContent(String content) {
		this.content = content;
	}
}
