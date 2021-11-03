package com.ebay.dss.zds.magic.pattern;

import com.ebay.dss.zds.magic.HandleContext;
import com.ebay.dss.zds.magic.HandleStateContext;
import com.ebay.dss.zds.magic.exception.ZetaMagicHandleException;

/**
 * Created by tatian on 2020-11-17.
 */
public interface PatternHandler {

  HandleStateContext handle(HandleContext handleContext) throws ZetaMagicHandleException;
}
