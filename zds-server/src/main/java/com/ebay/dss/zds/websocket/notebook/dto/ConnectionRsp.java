package com.ebay.dss.zds.websocket.notebook.dto;

import com.ebay.dss.zds.common.InterpreterRsp;
import com.ebay.dss.zds.model.ZetaResponse;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * Created by tatian on 2019-02-18.
 */
public class ConnectionRsp implements Serializable {

    @NotBlank
    private String userName;
    @NotBlank
    private String noteId;
    @NotBlank
    private String interpreter;
    private ZetaResponse<InterpreterRsp> details;

    public ConnectionRsp(String userName, String noteId, String interpreter, ZetaResponse<InterpreterRsp> details) {
        this.userName = userName;
        this.noteId = noteId;
        this.interpreter = interpreter;
        this.details = details;
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

    public ZetaResponse<InterpreterRsp> getDetails() {
        return details;
    }

    public void setDetails(ZetaResponse<InterpreterRsp> details) {
        this.details = details;
    }
}
