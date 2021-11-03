package com.ebay.dss.zds.auth.impl.jwt;

import com.ebay.dss.zds.common.LogUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;

public class JwtAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    private static final Logger logger = LogManager.getLogger();

    private final JwtAuthenticationExtractor extractor;

    public JwtAuthenticationFilter(RequestMatcher requiresAuthenticationRequestMatcher,
                                   JwtAuthenticationExtractor extractor) {
        super(requiresAuthenticationRequestMatcher);
        this.extractor = extractor;
    }

    private String getClientIp(HttpServletRequest request) {
        String clientAddr = "";
        if (Objects.nonNull(request)) {
            clientAddr = request.getHeader("X-FORWARDED-FOR");
            if (StringUtils.isBlank(clientAddr)) {
                clientAddr = request.getRemoteAddr();
            }
        }
        return clientAddr;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        logger.debug(getClientIp(request) + " auth with zeta token");
        String token = extractor.extract(request);
        logger.debug(getClientIp(request) + " extracted token: " + LogUtil.displayableSecret(token, 5, 5, 50));
        Authentication authentication = new JwtAuthenticationToken(token);
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
