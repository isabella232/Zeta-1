package com.ebay.dss.zds.auth.impl.manipulation;

import com.ebay.dss.zds.auth.impl.jwt.JwtAuthenticationExtractor;
import com.ebay.dss.zds.auth.impl.jwt.JwtAuthenticationToken;
import com.ebay.dss.zds.common.LogUtil;
import com.ebay.dss.zds.serverconfig.AuthConfig;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.ebay.dss.zds.auth.impl.manipulation.ManipulationAuthenticationToken.extractManipulator;

/**
 * Created by tatian on 2021/7/12.
 */
public class ManipulationAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

  private AuthConfig authConfig;

  public ManipulationAuthenticationFilter(RequestMatcher requiresAuthenticationRequestMatcher, AuthConfig authConfig) {
    super(requiresAuthenticationRequestMatcher);
    this.authConfig = authConfig;
  }

  @Override
  public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
    String manipulator = extractManipulator(request);
    Authentication authentication = new ManipulationAuthenticationToken(manipulator);
    return super.getAuthenticationManager().authenticate(authentication);
  }

  @Override
  protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {
    SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
    securityContext.setAuthentication(authResult);
    SecurityContextHolder.setContext(securityContext);
    chain.doFilter(request, response);
  }

}
