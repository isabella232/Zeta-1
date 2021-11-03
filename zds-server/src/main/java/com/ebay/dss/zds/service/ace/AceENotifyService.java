package com.ebay.dss.zds.service.ace;

import com.ebay.dss.zds.model.ace.AceEnotifies;
import com.ebay.dss.zds.model.ace.AceEnotifyReadOptions;
import com.ebay.dss.zds.model.ace.AceEnotifyOptions;

public interface AceENotifyService {

    AceEnotifyOptions.Product[] getAllEnotifyProducts();

    AceEnotifies findEnotifies(AceEnotifyOptions options);

    boolean setReadEnotifies(AceEnotifyReadOptions options);

    AceEnotifyOptions.Type[] getAllEnotifyTypes();
}
