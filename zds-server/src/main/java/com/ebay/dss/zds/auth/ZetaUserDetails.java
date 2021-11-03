package com.ebay.dss.zds.auth;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Date;

public class ZetaUserDetails implements UserDetails {

    private final String name;
    private final Collection<GrantedAuthority> authorities;
    private final Date expiration;

    public ZetaUserDetails(String name, Collection<GrantedAuthority> authorities, Date expiration) {
        this.name = name;
        this.authorities = authorities;
        this.expiration = expiration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof ZetaUserDetails)) return false;

        ZetaUserDetails that = (ZetaUserDetails) o;

        return new EqualsBuilder().append(name, that.name).append(authorities, that.authorities).append(expiration, that.expiration).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(name).append(authorities).append(expiration).toHashCode();
    }

    public Date getExpiration() {
        return expiration;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return name;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return expiration.after(new Date());
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
