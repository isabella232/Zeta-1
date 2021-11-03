package com.ebay.dss.zds.service.metatable;

import com.ebay.dss.zds.model.metatable.ZetaMetaTable;

import java.sql.SQLException;
import java.util.Objects;

public abstract class OperationCommand {

  public OperationReceiver operationReceiver;

  public OperationCommand nextHandler;

  public OperationCommand(OperationReceiver _operationReceiver) {
    this.operationReceiver = _operationReceiver;
  }

  public void setNextHandler(OperationCommand _nextHandler) {
    this.nextHandler = _nextHandler;
  }


  public Object handleOperation(ZetaMetaTable zetaMetaTable) {
    Object response = execute(zetaMetaTable);
    if (Objects.nonNull(this.nextHandler)) {
      response = this.nextHandler.handleOperation(zetaMetaTable);
    }
    return response;
  }

  public void handleOperation(ZetaMetaTable zetaMetaTable, Object data) {
    execute(zetaMetaTable, data);
    if (Objects.nonNull(this.nextHandler)) {
      this.nextHandler.handleOperation(zetaMetaTable, data);
    }
  }

  abstract Object execute(ZetaMetaTable zetaMetaTable);

  abstract void execute(ZetaMetaTable zetaMetaTable, Object data);

  protected Throwable handleSQLException(Exception e) {
    Throwable cause = e.getCause();
    while (Objects.nonNull(cause)) {
      if (cause instanceof SQLException
          || Objects.isNull(cause.getCause())) {
        break;
      }
      cause = cause.getCause();
    }
    return Objects.isNull(cause) ? e : cause;
  }

}
