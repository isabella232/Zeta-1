package com.ebay.dss.zds.model;

import com.ebay.dss.zds.interpreter.interpreters.Interpreter;

import javax.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.util.Map;

/**
 * @author shighuang
 */
public class ZetaJobRequest {

    private long id;
    private String notebookId;
    private String content;
    private Timestamp createDt;
    private Timestamp startDt;
    private Timestamp updateDt;
    private int livySessionId;
    private String livyJobUrl;
    private String proxyUser;
    private String status;
    private String preference;
    private String platform;

    public String getProxyUser() {
        return proxyUser;
    }

    public void setProxyUser(String proxyUser) {
        this.proxyUser = proxyUser;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getNotebookId() {
        return notebookId;
    }

    public void setNotebookId(String notebookId) {
        this.notebookId = notebookId;
    }

    public Timestamp getCreateDt() {
        return createDt;
    }

    public void setCreateDt(Timestamp createDt) {
        this.createDt = createDt;
    }

    public Timestamp getStartDt() {
        return startDt;
    }

    public void setStartDt(Timestamp startDt) {
        this.startDt = startDt;
    }

    public int getLivySessionId() {
        return livySessionId;
    }

    public void setLivySessionId(int livySessionId) {
        this.livySessionId = livySessionId;
    }

    public String getLivyJobUrl() {
        return livyJobUrl;
    }

    public void setLivyJobUrl(String livyJobUrl) {
        this.livyJobUrl = livyJobUrl;
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

    public Timestamp getUpdateDt() {
        return updateDt;
    }

    public void setUpdateDt(Timestamp updateDt) {
        this.updateDt = updateDt;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public void fillRequestContext(@NotNull ZetaNotebookPreference preference, @NotNull Interpreter interpreter) {
      Map<String, Object> notebookConnection = preference.notebookConnection;
      setProxyUser(ZetaNotebookPreference
              .getBatchAccount(notebookConnection).orElse("Unknown").toString());
      setPlatform(ZetaNotebookPreference
              .getPlatformIdentifier(notebookConnection).orElse("Unknown").toString());
      Map<String, Object> clonedConnection = preference.cloneNotebookConnection();
      ZetaNotebookPreference cloned = ZetaNotebookPreference.getInstance(clonedConnection);
      interpreter.propsDescribe(cloned.notebookConnection);
      setPreference(cloned.toJson());
    }
}
