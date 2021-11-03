package com.ebay.dss.zds.websocket.notebook.dto;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Map;

/**
 * Created by tatian on 2019-02-18.
 */
public class ConnectionReq implements Serializable {

    @NotBlank
    private String userName;

    @NotBlank
    private String noteId;

    @NotBlank
    private String interpreter;

    private boolean isCollectionAware = false;

    @Override
    public String toString() {
        return "ConnectionReq{" +
                "userName='" + userName + '\'' +
                ", noteId='" + noteId + '\'' +
                ", interpreter='" + interpreter + '\'' +
                ", isCollectionAware=" + isCollectionAware +
                '}';
    }

    public boolean getIsCollectionAware() {
        return isCollectionAware;
    }

    public void setIsCollectionAware(boolean isCollectionAware) {
        this.isCollectionAware = isCollectionAware;
    }

    private Map<String, String> prop;

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

    public Map<String, String> getProp() {
        return prop;
    }

    public void setProp(Map<String, String> prop) {
        this.prop = prop;
    }
}
