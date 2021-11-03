package com.ebay.dss.zds.service.metatable;

import com.ebay.dss.zds.model.metatable.ZetaMetaTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateCommand extends OperationCommand {

  private static final Logger LOGGER = LoggerFactory.getLogger(CreateCommand.class);

  public CreateCommand(OperationReceiver _operationReceiver) {
    super(_operationReceiver);
  }

  @Override
  public Object execute(ZetaMetaTable zetaMetaTable) {
    LOGGER.info("{} receive {} for table {}[{}]"
        , operationReceiver.getClass().getName(), this.getClass().getName()
        , zetaMetaTable.getMetaTableName(), zetaMetaTable.getId());
    try {
      this.operationReceiver.create(zetaMetaTable);
    } catch (RuntimeException e) {
      LOGGER.error("Create Command Fail", e);
      throw new RuntimeException(handleSQLException(e).getMessage());
    }
    return null;
  }

  @Override
  public void execute(ZetaMetaTable zetaMetaTable, Object data) {
  }
}
