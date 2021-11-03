package com.ebay.dss.zds.service.metatable;

import com.ebay.dss.zds.model.metatable.ZetaMetaTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateCommand extends OperationCommand {

  private static final Logger LOGGER = LoggerFactory.getLogger(UpdateCommand.class);

  public UpdateCommand(OperationReceiver _operationReceiver) {
    super(_operationReceiver);
  }

  @Override
  public Object execute(ZetaMetaTable zetaMetaTable) {
    return null;
  }

  @Override
  public void execute(ZetaMetaTable zetaMetaTable, Object data) {
    LOGGER.info("{} receive {} for table {}[{}]"
        , operationReceiver.getClass().getName(), this.getClass().getName()
        , zetaMetaTable.getMetaTableName(), zetaMetaTable.getId());
    try {
      this.operationReceiver.update(zetaMetaTable, data);
    } catch (RuntimeException e) {
      LOGGER.error("Update Command Fail", e);
      throw new RuntimeException(handleSQLException(e).getMessage());
    }
  }

}
