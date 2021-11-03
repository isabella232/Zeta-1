package com.ebay.dss.zds.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ZetaCookieFilter extends OncePerRequestFilter {

    private final ZetaCookieFactory zetaCookieFactory;

    public ZetaCookieFilter(ZetaCookieFactory zetaCookieFactory) {
        this.zetaCookieFactory = zetaCookieFactory;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        SecurityContext context = SecurityContextHolder.getContext();
        if (context != null) {
            inSecurityContextProcessCookies(context, request, response);
        }
        filterChain.doFilter(request, response);
    }

    private void inSecurityContextProcessCookies(SecurityContext context, HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = context.getAuthentication();
        if (auth != null) {
            processCookiesWhenAuthenticated(auth, request, response);
        }
    }

    private void processCookiesWhenAuthenticated(Authentication auth, HttpServletRequest request, HttpServletResponse response) {
        Object principal = auth.getPrincipal();
        if (principal instanceof ZetaUserDetails) {
            // for jwt token based request
            Cookie[] cookies = request.getCookies();
            if (!zetaCookieFactory.existNtCookie(cookies)) {
                response.addCookie(zetaCookieFactory.genNtCookie(((ZetaUserDetails) principal).getUsername()));
            }
        }
    }
}
