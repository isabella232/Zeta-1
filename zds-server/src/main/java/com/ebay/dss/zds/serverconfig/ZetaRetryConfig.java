package com.ebay.dss.zds.serverconfig;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@EnableRetry
@EnableAspectJAutoProxy(exposeProxy = true)
public class ZetaRetryConfig {

}
