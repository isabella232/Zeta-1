package com.ebay.dss.zds.auth.impl.manipulation;

import com.ebay.dss.zds.auth.impl.AbstractAuthImpl;
import com.ebay.dss.zds.serverconfig.AuthConfig;
import com.ebay.dss.zds.service.ZetaUserService;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.web.authentication.ForwardAuthenticationFailureHandler;
import org.springframework.security.web.authentication.ForwardAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.Arrays;

/**
 * Created by tatian on 2021/7/11.
 */
public class ManipulationAuthImpl extends AbstractAuthImpl {

  public ManipulationAuthImpl(AuthConfig authConfig, ZetaUserService userService) {

    super(authConfig, userService);
  }

  @Override
  public void configure(AuthenticationManager authenticationManager,
                        HttpSecurityBuilder httpSecurityBuilder,
                        String[] securedEndpoints,
                        String[] publicEndpoints,
                        String[] publicGetEndpoints) throws Exception {

    httpSecurityBuilder.addFilterBefore(this.authenticationAssigner(authenticationManager),
            UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(this.manipulationAuthenticationFilter(authenticationManager,
                    securedEndpoints, publicEndpoints, publicGetEndpoints),
                    UsernamePasswordAuthenticationFilter.class);
  }

  @Override
  public void configure(AuthenticationManagerBuilder auth) throws Exception {
    auth.authenticationProvider(new ManipulationAuthenticationProvider());
  }

  private RequestMatcher manipulationRequestMatcher(String[] securedEndpoints,
                                           String[] publicEndpoints,
                                           String[] publicGetEndpoints) {
    RequestMatcher[] publicMatchers = Arrays.stream(publicEndpoints)
            .map(AntPathRequestMatcher::new)
            .map(NegatedRequestMatcher::new)
            .toArray(RequestMatcher[]::new);

    RequestMatcher[] publicGetMatchers = Arrays.stream(publicGetEndpoints)
            .map(p -> new AntPathRequestMatcher(p, "GET"))
            .map(NegatedRequestMatcher::new)
            .toArray(RequestMatcher[]::new);

    RequestMatcher securedMatcher = new AntPathRequestMatcher("/**/*");
    RequestMatcher[] all = ArrayUtils.addAll(publicMatchers, publicGetMatchers);
    all = ArrayUtils.addAll(all, securedMatcher);

    return new AndRequestMatcher(all);
  }

  private ManipulationAuthenticationFilter manipulationAuthenticationFilter(AuthenticationManager authenticationManager,
                                                                            String[] securedEndpoints,
                                                                            String[] publicEndpoints,
                                                                            String[] publicGetEndpoints) throws Exception {
    RequestMatcher matcher = manipulationRequestMatcher(securedEndpoints, publicEndpoints, publicGetEndpoints);
    ManipulationAuthenticationFilter filter = new ManipulationAuthenticationFilter(matcher, authConfig);
    filter.setAuthenticationManager(authenticationManager);
    filter.setAuthenticationFailureHandler(new ForwardAuthenticationFailureHandler("/invalid"));
    return filter;
  }

  private RequestMatcher loginRequestMatcher() {
    return new AntPathRequestMatcher("/login");
  }

  private ManipulationAuthenticationAssigner authenticationAssigner(AuthenticationManager authenticationManager) throws Exception {
    RequestMatcher matcher = loginRequestMatcher();
    ManipulationAuthenticationAssigner filter = new ManipulationAuthenticationAssigner(matcher, authConfig);
    filter.setAuthenticationManager(authenticationManager);
    filter.setAuthenticationSuccessHandler(new ForwardAuthenticationSuccessHandler("/loginSuccess/token"));
    filter.setAuthenticationFailureHandler(new ForwardAuthenticationFailureHandler("/loginFailure"));
    return filter;
  }
}
