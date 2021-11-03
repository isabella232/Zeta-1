package com.ebay.dss.zds.auth;

import com.ebay.dss.zds.auth.impl.AbstractAuthImpl;
import com.ebay.dss.zds.auth.impl.manipulation.ManipulationAuthImpl;
import com.ebay.dss.zds.serverconfig.AuthConfig;
import com.ebay.dss.zds.service.ZetaUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;

/**
 * Created by tatian on 2021/7/11.
 */
@Component
public class AuthenticationConfigurator {

  private static final Logger log = LoggerFactory.getLogger(AuthenticationConfigurator.class);

  private final AuthConfig authConfig;
  private final ZetaUserService userService;
  private final AbstractAuthImpl authImpl;

  @Autowired
  public AuthenticationConfigurator(AuthConfig authConfig, ZetaUserService userService) {
    this.authConfig = authConfig;
    this.userService = userService;
    this.authImpl = createAuthImpl(authConfig, userService);
    log.info("Auth impl created: {}", this.authImpl.getClass().getName());
  }

  public void configure(AuthenticationManager authenticationManager,
                        HttpSecurityBuilder httpSecurityBuilder,
                        String[] securedEndpoints,
                        String[] publicEndpoints,
                        String[] publicGetEndpoints) throws Exception {
    authImpl.configure(authenticationManager, httpSecurityBuilder, securedEndpoints, publicEndpoints, publicGetEndpoints);
  }

  public void configure(AuthenticationManagerBuilder auth) throws Exception {
    authImpl.configure(auth);
  }

  @NotNull
  private AbstractAuthImpl createAuthImpl(AuthConfig authConfig, ZetaUserService userService) {
    try {
      String authClass = authConfig.getAuthContext().getAuthClass();
      log.info("Try initializing auth impl: {}", authClass);
      Class targetAuthClass = Class.forName(authClass);
      if (AbstractAuthImpl.class.isAssignableFrom(targetAuthClass)) {
        Object constructedClass = ((Class<? extends AbstractAuthImpl>) targetAuthClass)
                .getConstructor(AuthConfig.class, ZetaUserService.class)
                .newInstance(authConfig, userService);
        return (AbstractAuthImpl) constructedClass;
      } else {
        log.warn("Not a valid auth impl: {}, using ManipulationAuthImpl", targetAuthClass.getName());
        return new ManipulationAuthImpl(authConfig, userService);
      }
    } catch (Exception ex) {
      log.error("Failed to create auth impl: {}, using ManipulationAuthImpl", ex.getMessage());
      return new ManipulationAuthImpl(authConfig, userService);
    }
  }
}
