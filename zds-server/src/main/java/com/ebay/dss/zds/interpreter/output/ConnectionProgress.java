package com.ebay.dss.zds.interpreter.output;

import com.ebay.dss.zds.websocket.WebSocketResp;

import javax.validation.constraints.NotBlank;

/**
 * Created by tatian on 2019-07-01.
 */
public class ConnectionProgress {

    @NotBlank
    private String userName;
    @NotBlank
    private String noteId;
    @NotBlank
    private String interpreter;

    private double progress;

    @NotBlank
    private String message;

    public ConnectionProgress(String userName, String noteId, String interpreter, double progress, String message) {
        this.userName = userName;
        this.noteId = noteId;
        this.interpreter = interpreter;
        this.progress = progress;
        this.message = message;
    }

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

    public String getInterpreter() {
        return interpreter;
    }

    public void setInterpreter(String interpreter) {
        this.interpreter = interpreter;
    }

    public double getProgress() {
        return progress;
    }

    public void setProgress(double progress) {
        this.progress = progress;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public WebSocketResp<ConnectionProgress> toWebSocketResp() {
        return WebSocketResp.get(WebSocketResp.OP.CONNECTION_PROGRESS, this);
    }
}
