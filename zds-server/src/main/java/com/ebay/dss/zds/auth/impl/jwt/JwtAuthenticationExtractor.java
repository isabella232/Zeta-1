package com.ebay.dss.zds.auth.impl.jwt;

import com.ebay.dss.zds.serverconfig.AuthConfig;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class JwtAuthenticationExtractor {

    private static final String AUTH_VALUE_PREFIX = "Bearer ";
    private final AuthConfig.Jwt config;
    private final Pattern authCookiePattern;

    JwtAuthenticationExtractor(AuthConfig config) {
        this.config = config.getJwt();
        this.authCookiePattern = Pattern.compile(this.config.getAuthCookie() + "=([a-zA-Z0-9-_.]+)");
    }

    public String extract(HttpServletRequest request) {
        String token = extractFromHeader(request);
        if (StringUtils.isBlank(token)) {
            token = extractFromCookie(request);
        }
        return token;
    }

    String extractFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (StringUtils.equals(cookie.getName(), config.getAuthCookie())) {
                    return cookie.getValue();
                }
            }
        }
        return "";
    }

    String extractFromHeader(HttpServletRequest request) {
        String authValue = request.getHeader(config.getAuthHeader());
        if (StringUtils.isNotBlank(authValue) &&
                StringUtils.startsWith(authValue, AUTH_VALUE_PREFIX)) {
            return StringUtils.split(authValue)[1];
        }
        return "";
    }

    public String extract(StompHeaderAccessor accessor) {
        String authHeaderValue = accessor.getFirstNativeHeader(config.getAuthHeader());
        String token = extractFromHeader(authHeaderValue);
        if (StringUtils.isBlank(token)) {
            List<String> cookies = accessor.getNativeHeader(HttpHeaders.COOKIE);
            token = extractFromCookie(cookies);
        }
        return token;
    }

    private String extractFromHeader(String headerValue) {
        String jwtToken = "";
        if (StringUtils.isNotBlank(headerValue) &&
                StringUtils.startsWith(headerValue, AUTH_VALUE_PREFIX)) {
            jwtToken = StringUtils.split(headerValue)[1];
        }
        return jwtToken;
    }

    String extractFromCookie(List<String> cookies) {
        if (cookies != null && !cookies.isEmpty()) {
            for (String cookie : cookies) {
                if (StringUtils.isBlank(cookie)) {
                    continue;
                }
                Matcher tokenMatcher = authCookiePattern.matcher(cookie);
                if (tokenMatcher.matches()) {
                    return tokenMatcher.group(1);
                }
            }
        }
        return "";
    }

}
