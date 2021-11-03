package com.ebay.dss.zds.model;

import com.ebay.dss.zds.common.JsonUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by zhouhuang on 2018年5月10日
 */
public class JobResult {


	protected boolean status;

	@JsonProperty
	protected String output;

	protected String details;

	public JobResult(){

	}

	public JobResult(boolean status) {
		this.status = status;
	}

	public JobResult(boolean status, String output) {
		this.status = status;
		this.output = output;
	}


	public boolean isStatus() {
		return status;
	}

	public boolean getStatus() {
		return status;
	}

	public void setStatus(boolean status) {
		this.status = status;
	}

	public String getOutput() {
		return output;
	}

	public void setOutput(String output) {
		this.output = output;
	}

	public String getDetails() {
		return details;
	}

	public void setDetails(String details) {
		this.details = details;
	}

	public String toString() {
		return JsonUtil.toJson(this);
	}

}