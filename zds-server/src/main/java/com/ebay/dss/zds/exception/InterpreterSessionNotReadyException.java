package com.ebay.dss.zds.exception;

/**
 * Created by tatian on 2021/3/30.
 */
public class InterpreterSessionNotReadyException extends RuntimeException {

  public InterpreterSessionNotReadyException(String sessionId) {
    super("The session is not ready now: " + sessionId);
  }
}
