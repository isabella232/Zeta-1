package com.ebay.dss.zds.websocket.notebook.dto;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * Created by tatian on 2020-08-26.
 */
public class RecoverReq implements Serializable {

  @NotBlank
  private String noteId;

  public String getNoteId() {
    return noteId;
  }

  public void setNoteId(String noteId) {
    this.noteId = noteId;
  }
}
