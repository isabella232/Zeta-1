package com.ebay.dss.zds.model;


import org.hibernate.annotations.Immutable;
import org.json.JSONException;
import org.json.JSONObject;

import javax.persistence.Column;
import javax.persistence.Table;
import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author shighuang
 */
@Entity
@Table(name = "zeta_notebook")
@Immutable
public class ZetaNotebook implements Cloneable{
    @Id
    @Column
    private String id;
    @Column
    private String nt;
    @Column
    private String content;
    @Column
    private String title;
    @Column(name = "create_dt")
    private Timestamp createDt;
    @Column(name = "update_dt")
    private Timestamp updateDt;
    @Column
    private String status;
    @Column
    private String preference;
    @Column
    private String packages;
    @Column
    private String path;
    @Column
    private String platform;
    @Column(name = "proxy_user")
    private String proxyUser;
    @Column(name = "last_run_dt")
    private Timestamp lastRunDt;
    @Column
    private int opened;
    @Column
    private int seq;
    @Column
    private String sha;
    @Column(name = "git_repo")
    private String gitRepo;
    @Column(name = "public_role")
    @Enumerated(EnumType.STRING)
    private PublicRole publicRole;
    @Column(name = "public_referred")
    private String publicReferred;
    @Column(name = "nb_type")
    @Enumerated(EnumType.STRING)
    private NotebookType nbType;
    @Column(name = "collection_id")
    private String collectionId;

    public enum PublicRole {
        no_pub,
        pub,
        ref
    }

    public enum NotebookType {
        single,
        collection,
        sub_nb
    }

    public boolean ownedBy(String nt) {
        return this.nt.equals(nt);
    }

    public NotebookType getNbType() {
        return nbType;
    }

    public void setNbType(NotebookType nbType) {
        this.nbType = nbType;
    }

    public String getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(String collectionId) {
        this.collectionId = collectionId;
    }

    public PublicRole getPublicRole() {
        return publicRole;
    }

    public void setPublicRole(PublicRole publicRole) {
        this.publicRole = publicRole;
    }

    public String getPublicReferred() {
        return publicReferred;
    }

    public void setPublicReferred(String publicReferred) {
        this.publicReferred = publicReferred;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNt() {
        return nt;
    }

    public void setNt(String nt) {
        this.nt = nt;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPreference() {
        return preference;
    }

    public void setPreference(String preference) {
        this.preference = preference;
    }
    public void mergePreference(String preference) {
        if(this.preference == null || this.preference.equals("")){
            this.preference = preference;
            return ;
        }
        else{
            try {
                JSONObject srcObj = new JSONObject(this.preference);
                JSONObject newObj = new JSONObject(preference);
                Iterator keys = newObj.keys();
                String key = null;
                while (keys.hasNext()){
                    key = (String) keys.next();
                    srcObj.put(key, newObj.get(key));
                }
                this.preference = srcObj.toString();
            } catch (JSONException e){
                e.printStackTrace();
            }
            return;

        }
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getProxyUser() {
        return proxyUser;
    }

    public void setProxyUser(String proxyUser) {
        this.proxyUser = proxyUser;
    }

    public Timestamp getLastRunDt() {
        return lastRunDt;
    }

    public void setLastRunDt(Timestamp lastRunDt) {
        this.lastRunDt = lastRunDt;
    }

    public int getOpened() {
        return opened;
    }

    public void setOpened(int opened) {
        this.opened = opened;
    }

    public int getSeq() {
        return seq;
    }

    public void setSeq(int seq) {
        this.seq = seq;
    }

    public Map<String,Object> toMap(){
        Map<String,Object> map=new HashMap<>();
        map.put("id",id);
        map.put("title",title);
        map.put("content",content);
        map.put("path",path);
        return map;
    }

	/**
	 * @return the sha
	 */
	public String getSha() {
		return sha;
	}

	/**
	 * @param sha the sha to set
	 */
	public void setSha(String sha) {
		this.sha = sha;
	}

	/**
	 * @return the gitRepo
	 */
	public String getGitRepo() {
		return gitRepo;
	}

	/**
	 * @param gitRepo the gitRepo to set
	 */
	public void setGitRepo(String gitRepo) {
		this.gitRepo = gitRepo;
	}

    public String getPackages() {
        return packages;
    }

    public void setPackages(String packages) {
        this.packages = packages;
    }

}
