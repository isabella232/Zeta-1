package com.ebay.dss.zds.auth.impl.manipulation;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * Created by tatian on 2021/7/17.
 */
public class ManipulationAuthenticationProvider implements AuthenticationProvider {

  private final Collection<GrantedAuthority> fixedUserGrantedAuthority =
          Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"));

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    if (Objects.isNull(authentication.getPrincipal()) ||
            Objects.isNull(authentication.getCredentials())) {
      throw new AuthenticationServiceException("Invalid user id or token");
    }
    String nt = authentication.getPrincipal().toString();
    String token = authentication.getCredentials().toString();
    return new UsernamePasswordAuthenticationToken(nt, token, fixedUserGrantedAuthority);
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
  }

}
