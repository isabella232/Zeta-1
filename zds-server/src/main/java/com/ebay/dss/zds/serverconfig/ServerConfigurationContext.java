package com.ebay.dss.zds.serverconfig;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Created by tatian on 2019-07-17.
 */
public class ServerConfigurationContext {
    public static ThreadPoolTaskExecutor webSocketOutboundExecutor;
    public static ThreadPoolTaskExecutor webSocketInboundExecutor;
    public static ThreadPoolTaskExecutor webSocketMessageBrokerExecutor;
}
