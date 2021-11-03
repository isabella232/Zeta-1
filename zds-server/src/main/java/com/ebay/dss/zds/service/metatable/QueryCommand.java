package com.ebay.dss.zds.service.metatable;

import com.ebay.dss.zds.exception.MetaTableException;
import com.ebay.dss.zds.model.metatable.ZetaMetaTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class QueryCommand extends OperationCommand {

  private Logger LOGGER = LoggerFactory.getLogger(QueryCommand.class);

  public QueryCommand(OperationReceiver _operationReceiver) {
    super(_operationReceiver);
  }

  @Override
  public Object execute(ZetaMetaTable zetaMetaTable) {
    LOGGER.info("{} receive {} for table {}[{}]"
        , operationReceiver.getClass().getName(), this.getClass().getName()
        , zetaMetaTable.getMetaTableName(), zetaMetaTable.getId());
    try {
      return operationReceiver.query(zetaMetaTable);
    } catch (RuntimeException e) {
      throw new MetaTableException(handleSQLException(e).getMessage());
    }
  }

  @Override
  public void execute(ZetaMetaTable zetaMetaTable, Object data) {
  }

}
