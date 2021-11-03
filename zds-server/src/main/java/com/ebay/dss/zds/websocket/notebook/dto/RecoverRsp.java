package com.ebay.dss.zds.websocket.notebook.dto;

import com.ebay.dss.zds.state.model.NotebookTabState;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Map;

/**
 * Created by tatian on 2020-08-26.
 */
public class RecoverRsp implements Serializable {

  @NotBlank
  private String noteId;

  private Map<String, Object> tabState;

  public RecoverRsp(String noteId, Map<String, Object> tabState) {
    this.noteId = noteId;
    this.tabState = tabState;
  }

  public String getNoteId() {
    return noteId;
  }

  public void setNoteId(String noteId) {
    this.noteId = noteId;
  }

  public Map<String, Object> getTabState() {
    return tabState;
  }

  public void setTabState(Map<String, Object> tabState) {
    this.tabState = tabState;
  }
}
