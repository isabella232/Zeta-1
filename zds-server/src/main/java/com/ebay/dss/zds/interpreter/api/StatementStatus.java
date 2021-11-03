package com.ebay.dss.zds.interpreter.api;

import org.apache.zeppelin.interpreter.InterpreterResult;
import org.apache.zeppelin.interpreter.InterpreterResult.Code;

/**
 * Created by tatian on 2021/3/30.
 */
public enum StatementStatus {
  CREATED, RUNNING, SUCCESS, ERROR, CANCELED;

  public static StatementStatus fromInterpreterResult(InterpreterResult result) {
    if (result.code() == Code.SUCCESS) {
      return StatementStatus.SUCCESS;
    } else {
      return StatementStatus.ERROR;
    }
  }
}
