package com.ebay.dss.zds;

/**
 * Created by wenliu2 on 4/2/18.
 */

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableCaching
@EnableAspectJAutoProxy
@EnableConfigurationProperties
@EnableJpaRepositories(considerNestedRepositories = true)
public class Server {

  public static void main(String[] args) {
    SpringApplication.run(Server.class, args);
  }

}
