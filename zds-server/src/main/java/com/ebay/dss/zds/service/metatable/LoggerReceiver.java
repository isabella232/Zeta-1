package com.ebay.dss.zds.service.metatable;

import com.ebay.dss.zds.common.JsonUtil;
import com.ebay.dss.zds.model.metatable.MetaTableOperation.Operations;
import com.ebay.dss.zds.model.metatable.ZetaMetaTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service("LOGGER")
public class LoggerReceiver implements OperationReceiver {

  private static final Logger LOGGER = LoggerFactory.getLogger(LoggerReceiver.class);

  @Override
  public void create(ZetaMetaTable zetaMetaTable) {
  }

  @Override
  public void drop(ZetaMetaTable zetaMetaTable) {
    wrap(zetaMetaTable, Operations.DROP, null);
  }

  @Override
  public Object query(ZetaMetaTable zetaMetaTable) {
    return null;
  }

  @Override
  public void update(ZetaMetaTable zetaMetaTable, Object data) {
    wrap(zetaMetaTable, Operations.UPDATE, data);
  }

  private void wrap(ZetaMetaTable zetaMetaTable, Operations op, Object data) {
    LOGGER.info("USER-[{}] Operation-[{}] ZetaSheet-[{}] OLAPTable-[{}] HadoopTable-[{}] Platform-[{}] Data-[{}]"
        , zetaMetaTable.getLastModifier()
        , op
        , zetaMetaTable.getMetaTableName()
        , zetaMetaTable.getId().replace("-", "")
        , zetaMetaTable.getDb() + "." + zetaMetaTable.getTbl()
        , zetaMetaTable.getPlatform()
        , JsonUtil.toJson(data));
  }
}
