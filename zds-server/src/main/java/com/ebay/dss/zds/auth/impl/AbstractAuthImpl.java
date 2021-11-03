package com.ebay.dss.zds.auth.impl;

import com.ebay.dss.zds.serverconfig.AuthConfig;
import com.ebay.dss.zds.service.ZetaUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;

/**
 * Created by tatian on 2021/7/11.
 */
public abstract class AbstractAuthImpl {

  protected AuthConfig authConfig;
  protected com.ebay.dss.zds.service.ZetaUserService userService;

  public AbstractAuthImpl(AuthConfig authConfig, ZetaUserService userService) {
    this.authConfig = authConfig;
    this.userService = userService;
  }

  public abstract void configure(AuthenticationManager authenticationManager,
                                 HttpSecurityBuilder httpSecurityBuilder,
                                 String[] securedEndpoints,
                                 String[] publicEndpoints,
                                 String[] publicGetEndpoints) throws Exception;
  public abstract void configure(AuthenticationManagerBuilder auth) throws Exception;
}
