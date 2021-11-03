package com.ebay.dss.zds.model;

import com.ebay.dss.zds.common.SecretSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.sql.Timestamp;

/**
 * @author shighuang
 */
public class ZetaUser {

    private String nt;
    private String name;
    @JsonSerialize(using = SecretSerializer.class)
    private String token;
    @JsonSerialize(using = SecretSerializer.class)
    private String tdPass;
    @JsonSerialize(using = SecretSerializer.class)
	private String githubToken;
    private String preference;
    private Timestamp createDt;
    private Timestamp updateDt;
    private String batchAccounts;
    private int admin;
    @JsonSerialize(using = SecretSerializer.class)
    private String windowsAutoAccountPass;
    private boolean aceAdmin;

    public boolean isAceAdmin() {
        return aceAdmin;
    }

    public void setAceAdmin(boolean aceAdmin) {
        this.aceAdmin = aceAdmin;
    }

    public String getWindowsAutoAccountPass() {
        return windowsAutoAccountPass;
    }

    public void setWindowsAutoAccountPass(String windowsAutoAccountPass) {
        this.windowsAutoAccountPass = windowsAutoAccountPass;
    }

    public String getTdPass() {
        return tdPass;
    }

    public void setTdPass(String tdPass) {
        this.tdPass = tdPass;
    }

    public String getNt() {
        return nt;
    }

    public void setNt(String nt) {
        this.nt = nt;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getPreference() {
        return preference;
    }

    public void setPreference(String preference) {
        this.preference = preference;
    }

    public Timestamp getCreateDt() {
        return createDt;
    }

    public void setCreateDt(Timestamp createDt) {
        this.createDt = createDt;
    }

    public Timestamp getUpdateDt() {
        return updateDt;
    }

    public void setUpdateDt(Timestamp updateDt) {
        this.updateDt = updateDt;
    }

    public String getBatchAccounts() {
        return batchAccounts;
    }

    public void setBatchAccounts(String batchAccounts) {
        this.batchAccounts = batchAccounts;
    }

    public int getAdmin() {
        return admin;
    }

    public void setAdmin(int admin) {
        this.admin = admin;
    }

	/**
	 * @return the githubToken
	 */
	public String getGithubToken() {
		return githubToken;
	}

	/**
	 * @param githubToken the githubToken to set
	 */
	public void setGithubToken(String githubToken) {
		this.githubToken = githubToken;
	}
}
