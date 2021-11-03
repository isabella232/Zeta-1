package com.ebay.dss.zds.websocket.notebook.dto;

import com.ebay.dss.zds.common.JsonUtil;
import com.ebay.dss.zds.model.NotebookVariable;

import java.io.Serializable;
import java.util.Map;

/**
 * Created by tatian on 2020-10-27.
 */
public class NotebookVarsRefresh implements Serializable {

  private String notebookId;
  private Map<String, NotebookVariable> vars;

  public NotebookVarsRefresh(String notebookId, Map<String, NotebookVariable> vars) {
    this.notebookId = notebookId;
    this.vars = vars;
  }

  public String getNotebookId() {
    return notebookId;
  }

  public void setNotebookId(String notebookId) {
    this.notebookId = notebookId;
  }

  public Map<String, NotebookVariable> getVars() {
    return vars;
  }

  public void setVars(Map<String, NotebookVariable> vars) {
    this.vars = vars;
  }

  public String toJson() {
    return JsonUtil.GSON.toJson(this);
  }

  public static NotebookVarsRefresh fromJson(String json) {
    return JsonUtil.GSON.fromJson(json, NotebookVarsRefresh.class);
  }
}
