package com.ebay.dss.zds.common;

import com.ebay.dss.zds.websocket.AbstractWSController;
import com.ebay.dss.zds.websocket.WebSocketResp;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * Created by wenliu2 on 4/16/18.
 */
public class QueueDestination {
  private String userName;
  private SimpMessagingTemplate template;
  private String queue;

  public QueueDestination(String userName, SimpMessagingTemplate template, String queue) {
    this.userName = userName;
    this.template = template;
    this.queue = queue;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public void setTemplate(SimpMessagingTemplate template) {
    this.template = template;
  }

  public String getQueue() {
    return queue;
  }

  public void setQueue(String queue) {
    this.queue = queue;
  }

  public <T> void sendData(WebSocketResp<T> payload){
    this.template.convertAndSendToUser(getUserName(), getQueue(), payload);
  }
}
