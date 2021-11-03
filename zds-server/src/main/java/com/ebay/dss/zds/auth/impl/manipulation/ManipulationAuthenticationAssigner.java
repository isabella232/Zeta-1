package com.ebay.dss.zds.auth.impl.manipulation;

import com.ebay.dss.zds.common.LogUtil;
import com.ebay.dss.zds.serverconfig.AuthConfig;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * Created by tatian on 2021/7/12.
 */
public class ManipulationAuthenticationAssigner extends AbstractAuthenticationProcessingFilter {

  public ManipulationAuthenticationAssigner(RequestMatcher requiresAuthenticationRequestMatcher,
                                       AuthConfig config) {
    super(requiresAuthenticationRequestMatcher);
  }

  @Override
  public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
    if (HttpMethod.POST != HttpMethod.resolve(request.getMethod())) {
      return null;
    }
    logger.info(request.getRemoteAddr() + " try to login");
    String principal = ManipulationAuthenticationToken.extractManipulator(request);
    logger.info(request.getRemoteAddr() + " extracted nt: " + principal + ", token: " + principal);
    Authentication authentication = new UsernamePasswordAuthenticationToken(principal, principal);
    return super.getAuthenticationManager().authenticate(authentication);
  }
}
