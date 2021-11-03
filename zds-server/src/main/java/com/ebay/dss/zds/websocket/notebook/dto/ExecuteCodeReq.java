package com.ebay.dss.zds.websocket.notebook.dto;

import com.google.common.base.MoreObjects;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Map;
import java.util.Properties;

/**
 * Created by wenliu2 on 4/16/18.
 */
public class ExecuteCodeReq implements Serializable {
    @NotBlank
    private String reqId;

    @NotBlank
    private String notebookId;
    @NotBlank
    private String codes;

    @NotBlank
    private String interpreter;

    // for multi language notebook, decide if interpreter create by collection notebook id or sub notebook id
    private boolean isCollectionAware = false;

    private Map<String, String> prop;

    public boolean getIsCollectionAware() {
        return isCollectionAware;
    }

    public void setIsCollectionAware(boolean isCollectionAware) {
        this.isCollectionAware = isCollectionAware;
    }

    public String getNotebookId() {
        return notebookId;
    }

    public void setNotebookId(String notebookId) {
        this.notebookId = notebookId;
    }

    public String getCodes() {
        return codes;
    }

    public void setCodes(String codes) {
        this.codes = codes;
    }

    public String getReqId() {
        return reqId;
    }

    public void setReqId(String reqId) {
        this.reqId = reqId;
    }

    public String getInterpreter() {
        return interpreter;
    }

    public void setInterpreter(String interpreter) {
        this.interpreter = interpreter;
    }

    public void setProp(Map<String, String> prop) {
        this.prop = prop;
    }

    public Map<String, String> getProp() {
        return prop;
    }

    public String getProperty(String key) {
        return prop.get(key);
    }

    public String getProperty(String key, String defaultValue) {
        return prop.getOrDefault(key, defaultValue);
    }



    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("notebookId", notebookId)
                .add("reqId", reqId)
                .add("codes", codes)
                .add("interpreter", interpreter)
                .add("isCollectionAware", isCollectionAware)
                .toString();
    }
}
