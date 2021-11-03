package com.ebay.dss.zds.exception;

/**
 * Created by tatian on 2021/3/30.
 */
public class InterpreterSessionNotFoundException extends RuntimeException {

  public InterpreterSessionNotFoundException(String sessionId) {
    super("The session is not found: " + sessionId);
  }
}
