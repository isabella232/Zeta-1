package com.ebay.dss.zds.auth;

import com.ebay.dss.zds.serverconfig.AuthConfig;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.servlet.http.Cookie;

public class ZetaCookieFactory {

    private final AuthConfig config;

    public ZetaCookieFactory(AuthConfig config) {
        this.config = config;
    }

    public boolean existNtCookie(Cookie[] cookies) {
        return existCookie(config.getJwt().getNtCookie(), cookies);
    }

    public boolean existTokenCookie(Cookie[] cookies) {
        return existCookie(config.getJwt().getAuthCookie(), cookies);
    }

    private boolean existCookie(String name, Cookie[] cookies) {
        if (cookies == null) {
            return false;
        }
        for (Cookie cookie : cookies) {
            if (StringUtils.equalsIgnoreCase(name, cookie.getName())) {
                return true;
            }
        }
        return false;
    }

    public Cookie genTokenCookie(String token) {
        Cookie ntCookie = new Cookie(config.getJwt().getAuthCookie(), token);
        ntCookie.setPath("/");
        ntCookie.setMaxAge(config.getJwt().getExpirationTime() * 60);
        return ntCookie;
    }

    public Cookie genNtCookie(String nt) {
        Cookie ntCookie = new Cookie(config.getJwt().getNtCookie(), nt);
        ntCookie.setPath("/");
        ntCookie.setMaxAge(14 * 24 * 3600);
        return ntCookie;
    }

}
