package com.ebay.dss.zds.interpreter.interpreters.jdbc;

import com.ebay.dss.zds.interpreter.input.ExecutionContext;

import java.sql.PreparedStatement;

/**
 * Created by tatian on 2020-05-07.
 */
public interface StatementContext {

  ExecutionContext getExecutionContext();
  PreparedStatement getPreparedStatement();
}
