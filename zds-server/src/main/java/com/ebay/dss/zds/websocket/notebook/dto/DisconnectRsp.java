package com.ebay.dss.zds.websocket.notebook.dto;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * Created by tatian on 2019-03-13.
 */
public class DisconnectRsp implements Serializable {

    @NotBlank
    private String noteId;

    @NotBlank
    private String details;

    @NotBlank
    private boolean success;

    public DisconnectRsp(String noteId, String details, boolean success) {
        this.noteId = noteId;
        this.details = details;
        this.success = success;
    }

    public String getNoteId() {
        return noteId;
    }

    public void setNoteId(String noteId) {
        this.noteId = noteId;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
