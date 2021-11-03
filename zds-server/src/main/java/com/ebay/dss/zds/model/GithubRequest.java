package com.ebay.dss.zds.model;

import java.util.List;
import java.util.Map;

/**
 * Created by zhouhuang on 2018年12月14日
 */
public class GithubRequest {

	private String nt;

	private String url;

	private String branch;
	
	private String newBranch;

	private String folderSha;

	private String wsPath;

	private String tag;

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	private String searchKeyword;

	private String commitMessage;

	private String token;

	private String gitApi;

	private List<Map<String, String>> fileList;

	/**
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * @param url
	 *            the url to set
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * @return the branch
	 */
	public String getBranch() {
		return branch;
	}

	/**
	 * @param branch
	 *            the branch to set
	 */
	public void setBranch(String branch) {
		this.branch = branch;
	}

	/**
	 * @return the tag
	 */
	public String getTag() {
		return tag;
	}

	/**
	 * @param tag
	 *            the tag to set
	 */
	public void setTag(String tag) {
		this.tag = tag;
	}

	/**
	 * @return the nt
	 */
	public String getNt() {
		return nt;
	}

	/**
	 * @param nt
	 *            the nt to set
	 */
	public void setNt(String nt) {
		this.nt = nt;
	}

	/**
	 * @return the searchKeyword
	 */
	public String getSearchKeyword() {
		return searchKeyword;
	}

	/**
	 * @param searchKeyword
	 *            the searchKeyword to set
	 */
	public void setSearchKeyword(String searchKeyword) {
		this.searchKeyword = searchKeyword;
	}

	/**
	 * @return the commitMessage
	 */
	public String getCommitMessage() {
		return commitMessage;
	}

	/**
	 * @param commitMessage
	 *            the commitMessage to set
	 */
	public void setCommitMessage(String commitMessage) {
		this.commitMessage = commitMessage;
	}

	/**
	 * @return the wsPath
	 */
	public String getWsPath() {
		return wsPath;
	}

	/**
	 * @param wsPath
	 *            the wsPath to set
	 */
	public void setWsPath(String wsPath) {
		this.wsPath = wsPath;
	}

	/**
	 * @return the fileList
	 */
	public List<Map<String, String>> getFileList() {
		return fileList;
	}

	/**
	 * @param fileList the fileList to set
	 */
	public void setFileList(List<Map<String, String>> fileList) {
		this.fileList = fileList;
	}

	/**
	 * @return the newBranch
	 */
	public String getNewBranch() {
		return newBranch;
	}

	/**
	 * @param newBranch the newBranch to set
	 */
	public void setNewBranch(String newBranch) {
		this.newBranch = newBranch;
	}

	/**
	 * @return the folderSha
	 */
	public String getFolderSha() {
		return folderSha;
	}

	/**
	 * @param folderSha the folderSha to set
	 */
	public void setFolderSha(String folderSha) {
		this.folderSha = folderSha;
	}

	public String getGitApi() {
		return gitApi;
	}

	public void setGitApi(String gitApi) {
		this.gitApi = gitApi;
	}
}
