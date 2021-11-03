package com.ebay.dss.zds.model;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import java.util.Set;

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id", scope = Table.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Table {

	private Long id;

	private String tableName;

	private String databaseName;

	private String platformName;

	private Long rowCount;

	private Long tableSize;

	private String tableType;

	private String etlId;

	private String saCode;

	private String saName;

	private String batchAccount;

	private String ownerNt;

	private Set<Column> columns;

	public Table() {
	}

	public Table(String tableName, String databaseName, String platformName) {
		this.tableName = tableName;
		this.databaseName = databaseName;
		this.platformName = platformName;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public String getDatabaseName() {
		return databaseName;
	}

	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}

	public String getPlatformName() {
		return platformName;
	}

	public void setPlatformName(String platformName) {
		this.platformName = platformName;
	}

	public Long getRowCount() {
		return rowCount;
	}

	public void setRowCount(Long rowCount) {
		this.rowCount = rowCount;
	}

	public Long getTableSize() {
		return tableSize;
	}

	public void setTableSize(Long tableSize) {
		this.tableSize = tableSize;
	}

	public String getTableType() {
		return tableType;
	}

	public void setTableType(String tableType) {
		this.tableType = tableType;
	}

	public String getEtlId() {
		return etlId;
	}

	public void setEtlId(String etlId) {
		this.etlId = etlId;
	}

	public String getSaCode() {
		return saCode;
	}

	public void setSaCode(String saCode) {
		this.saCode = saCode;
	}

	public String getSaName() {
		return saName;
	}

	public void setSaName(String saName) {
		this.saName = saName;
	}

	public String getBatchAccount() {
		return batchAccount;
	}

	public void setBatchAccount(String batchAccount) {
		this.batchAccount = batchAccount;
	}

	public String getOwnerNt() {
		return ownerNt;
	}

	public void setOwnerNt(String ownerNt) {
		this.ownerNt = ownerNt;
	}

	public Set<Column> getColumns() {
		return columns;
	}

	public void setColumns(Set<Column> columns) {
		this.columns = columns;
	}

	public String getFullName() {
		return this.databaseName + "." + this.tableName;
	}
}
