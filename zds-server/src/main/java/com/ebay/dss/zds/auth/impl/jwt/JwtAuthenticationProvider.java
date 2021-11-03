package com.ebay.dss.zds.auth.impl.jwt;

import com.ebay.dss.zds.auth.ZetaUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;

@Component
public class JwtAuthenticationProvider implements AuthenticationProvider {

    private final JwtAuthenticationService jwtAuthenticationService;
    private final Collection<GrantedAuthority> fixedUserGrantedAuthority =
            Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"));

    @Autowired
    public JwtAuthenticationProvider(JwtAuthenticationService jwtAuthenticationService) {
        this.jwtAuthenticationService = jwtAuthenticationService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String jwtToken = (String) authentication.getCredentials();
        ZetaUserDetails user = jwtAuthenticationService.authenticateAndGetDetails(jwtToken);
        return new UsernamePasswordAuthenticationToken(user, jwtToken, user.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return JwtAuthenticationToken.class.isAssignableFrom(authentication);
    }

}
