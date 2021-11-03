package com.ebay.dss.zds.websocket.notebook.dto;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * Created by tatian on 2019-03-11.
 */
public class DisconnectReq implements Serializable {

    @NotBlank
    private String userName;

    @NotBlank
    private String noteId;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getNoteId() {
        return noteId;
    }

    public void setNoteId(String noteId) {
        this.noteId = noteId;
    }

}
