package com.ebay.dss.zds.magic;

import com.ebay.dss.zds.common.QueueDestination;
import com.ebay.dss.zds.interpreter.output.result.JsonResult;
import com.ebay.dss.zds.model.ZetaNotebook;

/**
 * Created by tatian on 2020-11-17.
 */
public interface VariableHandlerCallback {

  public static final VariableHandlerCallback DO_NOTHING = (JsonResult successResult,
                                                             QueueDestination dest,
                                                             ZetaNotebook notebook) -> {
  };

  void onSuccess(JsonResult successResult,
                 QueueDestination dest,
                 ZetaNotebook notebook) throws Exception;
}
