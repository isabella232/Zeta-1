package com.ebay.dss.zds.model;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id", scope = Column.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Column {

	private Long id;

	private Integer columnid;

	private String columnname;

	private String dataType;

	private String columntype;

	private Integer columnlength;

	private Integer decimaltotaldigits;

	private Integer decimalfractionaldigits;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getDataType() {
		return dataType;
	}

	public void setDataType(String dataType) {
		this.dataType = dataType;
	}

	/**
	 * @return the columntype
	 */
	public String getColumntype() {
		return columntype;
	}

	/**
	 * @param columntype
	 *            the columntype to set
	 */
	public void setColumntype(String columntype) {
		this.columntype = columntype;
	}

	/**
	 * @return the columnlength
	 */
	public Integer getColumnlength() {
		return columnlength;
	}

	/**
	 * @param columnlength
	 *            the columnlength to set
	 */
	public void setColumnlength(Integer columnlength) {
		this.columnlength = columnlength;
	}

	/**
	 * @return the columnid
	 */
	public Integer getColumnid() {
		return columnid;
	}

	/**
	 * @param columnid
	 *            the columnid to set
	 */
	public void setColumnid(Integer columnid) {
		this.columnid = columnid;
	}

	/**
	 * @return the columnname
	 */
	public String getColumnname() {
		return columnname;
	}

	/**
	 * @param columnname
	 *            the columnname to set
	 */
	public void setColumnname(String columnname) {
		this.columnname = columnname;
	}

	/**
	 * @return the decimaltotaldigits
	 */
	public Integer getDecimaltotaldigits() {
		return decimaltotaldigits;
	}

	/**
	 * @param decimaltotaldigits
	 *            the decimaltotaldigits to set
	 */
	public void setDecimaltotaldigits(Integer decimaltotaldigits) {
		this.decimaltotaldigits = decimaltotaldigits;
	}

	/**
	 * @return the decimalfractionaldigits
	 */
	public Integer getDecimalfractionaldigits() {
		return decimalfractionaldigits;
	}

	/**
	 * @param decimalfractionaldigits
	 *            the decimalfractionaldigits to set
	 */
	public void setDecimalfractionaldigits(Integer decimalfractionaldigits) {
		this.decimalfractionaldigits = decimalfractionaldigits;
	}

}
