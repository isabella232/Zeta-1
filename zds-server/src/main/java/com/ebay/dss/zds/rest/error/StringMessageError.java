package com.ebay.dss.zds.rest.error;

/**
 * Created by wenliu2 on 4/3/18.
 */
public class StringMessageError extends BaseErrorDetail{

  private String originalMessage;
  private String message;

  public String getOriginalMessage() {
    return originalMessage;
  }

  public StringMessageError setOriginalMessage(String originalMessage) {
    this.originalMessage = originalMessage;
    return this;
  }

  public StringMessageError(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public static StringMessageError from(String message){
    return new StringMessageError(message);
  }

  public static StringMessageError from(Throwable t){
    return new StringMessageError(t.getMessage());
  }
}
