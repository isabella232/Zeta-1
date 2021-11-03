package com.ebay.dss.zds.auth.impl.manipulation;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AbstractAuthenticationToken;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

/**
 * Created by tatian on 2021/7/12.
 */
public class ManipulationAuthenticationToken extends AbstractAuthenticationToken {

  public static String ANONYMOUS = "anonymous";
  public static String PRINCIPAL_KEY = "zeta-nt";

  private String manipulator;

  public static ManipulationAuthenticationToken anonymous() {
    return new ManipulationAuthenticationToken(ANONYMOUS);
  }

  public ManipulationAuthenticationToken(String manipulator) {
    super(null);
    super.setAuthenticated(false);
  }

  @Override
  public Object getCredentials() {
    return manipulator;
  }

  @Override
  public Object getPrincipal() {
    return manipulator;
  }

  public static String extractManipulator(HttpServletRequest request) {
    String token = extractFromHeader(request);
    if (StringUtils.isBlank(token)) {
      token = extractFromCookie(request);
    }
    if (StringUtils.isBlank(token)) {
      token = ManipulationAuthenticationToken.ANONYMOUS;
    }
    return token;
  }

  public static String extractFromCookie(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (StringUtils.equals(cookie.getName(), ManipulationAuthenticationToken.PRINCIPAL_KEY)) {
          return cookie.getValue();
        }
      }
    }
    return "";
  }

  public static String extractFromHeader(HttpServletRequest request) {
    return request.getHeader(ManipulationAuthenticationToken.PRINCIPAL_KEY);
  }
}
