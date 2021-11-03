package com.ebay.dss.zds.interpreter.api;

import com.ebay.dss.zds.interpreter.interpreters.Interpreter;

/**
 * Created by tatian on 2021/3/31.
 */
public interface InterpreterCreator {

  Interpreter create() throws Exception;
  void destroy(boolean async);
}
