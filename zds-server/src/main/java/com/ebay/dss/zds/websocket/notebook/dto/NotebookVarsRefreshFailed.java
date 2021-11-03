package com.ebay.dss.zds.websocket.notebook.dto;

import com.ebay.dss.zds.common.JsonUtil;
import com.ebay.dss.zds.model.NotebookVariable;

import java.io.Serializable;
import java.util.Map;

/**
 * Created by tatian on 2020-10-27.
 */
public class NotebookVarsRefreshFailed implements Serializable {

  private String notebookId;
  private String reason;

  public NotebookVarsRefreshFailed(String notebookId, String reason) {
    this.notebookId = notebookId;
    this.reason = reason;
  }

  public String getNotebookId() {
    return notebookId;
  }

  public void setNotebookId(String notebookId) {
    this.notebookId = notebookId;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public static NotebookVarsRefreshFailed fromJson(String json) {
    return JsonUtil.GSON.fromJson(json, NotebookVarsRefreshFailed.class);
  }
}
