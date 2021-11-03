package com.ebay.dss.zds.service.metatable;

import com.ebay.dss.zds.model.metatable.ZetaMetaTable;

public class OperationInvoker {

  public Object action(OperationCommand command
      , ZetaMetaTable zetaMetaTable) {
    return command.handleOperation(zetaMetaTable);
  }

  public void action(OperationCommand command
      , ZetaMetaTable zetaMetaTable, Object data) {
    command.handleOperation(zetaMetaTable, data);
  }

}
