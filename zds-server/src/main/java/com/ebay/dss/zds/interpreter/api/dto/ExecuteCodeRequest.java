package com.ebay.dss.zds.interpreter.api.dto;

import com.ebay.dss.zds.common.JsonUtil;

/**
 * Created by tatian on 2021/3/30.
 */
public class ExecuteCodeRequest {
  public String sessionId;
  public String code;

  public ExecuteCodeRequest() {
  }

  public ExecuteCodeRequest(String sessionId, String code) {
    this.sessionId = sessionId;
    this.code = code;
  }

  public String toJson() {
    return JsonUtil.GSON.toJson(this);
  }
}
