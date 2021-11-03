package com.ebay.dss.zds.auth.impl.jwt;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;

public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private String jwtToken;
    private String nt;

    public JwtAuthenticationToken(String jwtToken) {
        super(null);
        this.jwtToken = jwtToken;
        super.setAuthenticated(false);
    }

    public JwtAuthenticationToken(String nt, String jwtToken) {
        super(extractAuthorities());
        this.nt = nt;
        this.jwtToken = jwtToken;
        super.setAuthenticated(true);
    }

    private static Collection<GrantedAuthority> extractAuthorities() {
        // a fixed USER authority
        return Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public Object getCredentials() {
        return jwtToken;
    }

    @Override
    public Object getPrincipal() {
        return nt;
    }
}
