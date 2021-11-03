package com.ebay.dss.zds.message.queue;

import com.ebay.dss.zds.message.ZetaEvent;
import com.ebay.dss.zds.message.ZetaEventListener;

import javax.validation.constraints.NotNull;

/**
 * Created by tatian on 2019-07-23.
 */
public interface EventQueue {

    EventQueue start();

    boolean post(@NotNull ZetaEvent zetaEvent);

    boolean addListener(ZetaEventListener listener);

    boolean removeListener(ZetaEventListener listener);

    void removeAllListener();

    void stop();
}
