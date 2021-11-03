package com.ebay.dss.zds.interpreter.api.dto;

import com.ebay.dss.zds.common.JsonUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by tatian on 2021/3/30.
 */
public class InterpreterSessionRequest {
  private String noteId;
  private String userId;
  private String interpreter;
  private Map<String, String> props = new HashMap<>();

  public String getNoteId() {
    return noteId;
  }

  public void setNoteId(String noteId) {
    this.noteId = noteId;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getInterpreter() {
    return interpreter;
  }

  public void setInterpreter(String interpreter) {
    this.interpreter = interpreter;
  }

  public Map<String, String> getProps() {
    return props;
  }

  public void setProps(Map<String, String> props) {
    this.props = props;
  }

  public void mergeProps(Map<String, String> props) {
    this.props.putAll(props);
  }

  public void setProperty(String key, String value) {
    this.props.put(key, value);
  }

  public String toJson() {
    return JsonUtil.toJson(this);
  }

  public boolean reuseSession() {
    return Boolean.valueOf(props.getOrDefault("reuse", "false"));
  }
}
