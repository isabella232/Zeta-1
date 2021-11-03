package com.ebay.dss.zds.service.metatable;

import com.ebay.dss.zds.model.metatable.ZetaMetaTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DropCommand extends OperationCommand {

  private static final Logger LOGGER = LoggerFactory.getLogger(DropCommand.class);

  public DropCommand(OperationReceiver _operationReceiver) {
    super(_operationReceiver);
  }

  @Override
  public Object execute(ZetaMetaTable zetaMetaTable) {
    LOGGER.info("{} receive {} for table {}[{}]"
        , operationReceiver.getClass().getName(), this.getClass().getName()
        , zetaMetaTable.getMetaTableName(), zetaMetaTable.getId());
    this.operationReceiver.drop(zetaMetaTable);
    return null;
  }

  @Override
  public void execute(ZetaMetaTable zetaMetaTable, Object data) {
  }
}
