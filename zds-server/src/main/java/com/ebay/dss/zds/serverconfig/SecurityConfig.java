package com.ebay.dss.zds.serverconfig;

import com.ebay.dss.zds.auth.AuthenticationConfigurator;
import com.ebay.dss.zds.auth.ZetaCookieFactory;
import com.ebay.dss.zds.auth.ZetaCookieFilter;
import com.ebay.dss.zds.auth.impl.jwt.JwtAuthenticationExtractor;
import com.ebay.dss.zds.auth.impl.jwt.JwtAuthenticationFilter;
import com.ebay.dss.zds.auth.impl.jwt.JwtAuthenticationProvider;
import com.ebay.dss.zds.service.ZetaUserService;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.AbstractRequestMatcherRegistry;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer.AuthorizedUrl;
import org.springframework.security.web.authentication.ForwardAuthenticationFailureHandler;
import org.springframework.security.web.authentication.ForwardAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.Arrays;

/**
 * Created by wenliu2 on 4/10/18.
 */
@Configuration
@EnableWebSecurity
@EnableScheduling
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

  private final String[] securedEndpoints;
  private final String[] publicEndpoints;
  private final String[] publicGetEndpoints;
  private final AuthConfig authConfig;
  private final ZetaUserService zetaUserService;
  private final AuthenticationConfigurator configurator;

  @Autowired
  public SecurityConfig(@Value("${zds.server.secured.endpoints}") String[] securedEndpoints,
                        @Value("${zds.server.public.endpoints}") String[] publicEndpoints,
                        @Value("${zds.server.public.get-endpoints}") String[] publicGetEndpoints,
                        AuthConfig authConfig,
                        ZetaUserService zetaUserService) {
    this.securedEndpoints = securedEndpoints;
    this.publicEndpoints = publicEndpoints;
    this.publicGetEndpoints = publicGetEndpoints;
    this.authConfig = authConfig;
    this.zetaUserService = zetaUserService;
    this.configurator = new AuthenticationConfigurator(authConfig, zetaUserService);
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.csrf().disable();
    http.cors();
    AbstractRequestMatcherRegistry registry = http.authorizeRequests();
    for (String securedEndpoint : securedEndpoints) {
      ((AuthorizedUrl) registry.antMatchers(securedEndpoint)).authenticated();
    }
    for (String publicEndpoint : publicEndpoints) {
      ((AuthorizedUrl) registry.antMatchers(publicEndpoint)).permitAll();
    }
    for (String publicGetEndpoint : publicGetEndpoints) {
      ((AuthorizedUrl) registry
              .antMatchers(HttpMethod.GET, publicGetEndpoint))
              .permitAll();
    }
    this.configurator.configure(this.authenticationManager(),
            ((AuthorizedUrl) registry.anyRequest()).authenticated().and(),
            securedEndpoints,
            publicEndpoints,
            publicGetEndpoints);
  }

  @Override
  protected void configure(AuthenticationManagerBuilder auth) throws Exception {
    this.configurator.configure(auth);
  }

  @Bean
  @Override
  public AuthenticationManager authenticationManagerBean() throws Exception {
    return super.authenticationManagerBean();
  }
}
