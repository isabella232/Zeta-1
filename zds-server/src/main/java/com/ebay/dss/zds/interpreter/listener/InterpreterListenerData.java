package com.ebay.dss.zds.interpreter.listener;

/**
 * Created by wenliu2 on 4/26/18.
 */
public class InterpreterListenerData {
  //private String uniqueId;
  //private int seq; // the order of the statement in the who sqls (separated by ';')
  //String sql;  //sql code of the statement
  int statementId; //the livy statement id
  int sessionId;  //the livy session id
  String sparkJobUrl;

  //InterpreterResult result;

  public final static InterpreterListenerData Empty = new InterpreterListenerData();

  public int getStatementId() {
    return statementId;
  }

  public void setStatementId(int statementId) {
    this.statementId = statementId;
  }

  public int getSessionId() {
    return sessionId;
  }

  public void setSessionId(int sessionId) {
    this.sessionId = sessionId;
  }

  public String getSparkJobUrl() {
    return sparkJobUrl;
  }

  public void setSparkJobUrl(String sparkJobUrl) {
    this.sparkJobUrl = sparkJobUrl;
  }
}
