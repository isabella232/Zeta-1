package com.ebay.dss.zds.cluster;

import com.ebay.dss.zds.websocket.WebSocketResp;

import java.io.Serializable;

/**
 * Created by tatian on 2021-02-03.
 */
public class WebSocketRespWrapper<T> implements Serializable {

  private String user;
  private String name;
  private WebSocketResp<T> webSocketResp;

  public WebSocketRespWrapper(String user, String name, WebSocketResp<T> webSocketResp) {
    this.user = user;
    this.name = name;
    this.webSocketResp = webSocketResp;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public WebSocketResp<T> getWebSocketResp() {
    return webSocketResp;
  }

  public void setWebSocketResp(WebSocketResp<T> webSocketResp) {
    this.webSocketResp = webSocketResp;
  }
}
