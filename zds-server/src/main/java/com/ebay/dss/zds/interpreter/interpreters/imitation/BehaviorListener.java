package com.ebay.dss.zds.interpreter.interpreters.imitation;

import com.ebay.dss.zds.interpreter.listener.InterpreterListener;

import java.util.Properties;

/**
 * Created by tatian on 2020-12-02.
 */
public interface BehaviorListener {

  Behavior onConstruct(Properties context);
  Behavior onInterpret(String code, String paragraphId);
  Behavior onInterpret(String code, String paragraphId, InterpreterListener listener);
}
