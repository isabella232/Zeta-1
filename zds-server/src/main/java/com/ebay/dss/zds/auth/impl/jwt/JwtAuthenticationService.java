package com.ebay.dss.zds.auth.impl.jwt;

import com.ebay.dss.zds.auth.JwtTokenFactory;
import com.ebay.dss.zds.auth.ZetaUserDetails;
import io.jsonwebtoken.JwtException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthenticationService {

    private static final Logger logger = LogManager.getLogger();

    private final JwtTokenFactory tokenFactory;

    @Autowired
    public JwtAuthenticationService(JwtTokenFactory tokenFactory) {
        this.tokenFactory = tokenFactory;
    }

    public ZetaUserDetails authenticateAndGetDetails(String jwtToken) {
        ZetaUserDetails user = extractDetails(jwtToken);
        logger.debug("this token is claimed by " + user.getUsername());
        return user;
    }

    private ZetaUserDetails extractDetails(String jwtToken) {
        try {
            return tokenFactory.verify(jwtToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new AuthenticationServiceException(e.getMessage());
        }
    }

}
