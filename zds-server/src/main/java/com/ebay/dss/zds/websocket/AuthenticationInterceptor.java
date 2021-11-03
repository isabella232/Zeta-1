package com.ebay.dss.zds.websocket;

import com.ebay.dss.zds.auth.ZetaUserDetails;
import com.ebay.dss.zds.auth.impl.jwt.JwtAuthenticationExtractor;
import com.ebay.dss.zds.auth.impl.jwt.JwtAuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

/**
 * Created by wenliu2 on 4/10/18.
 */
@Component
public class AuthenticationInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationInterceptor.class);

    private static final String AUTH_VALUE_PREFIX = "Bearer";
    private final JwtAuthenticationService jwtAuthenticationService;
    private final JwtAuthenticationExtractor extractor;

    @Autowired
    public AuthenticationInterceptor(JwtAuthenticationService jwtAuthenticationService,
                                     JwtAuthenticationExtractor extractor) {
        this.jwtAuthenticationService = jwtAuthenticationService;
        this.extractor = extractor;
    }

    @Override
    public Message<?> preSend(final Message<?> message, final MessageChannel channel) throws AuthenticationException {
        final StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT == accessor.getCommand()) {
            authAndStoreSessionWhenConnect(accessor);
            if (log.isTraceEnabled()) {
                log.trace("Websocket authenticate done.");
            }
        }
        return message;
    }

    private void authAndStoreSessionWhenConnect(StompHeaderAccessor accessor) {
        String jwtToken = extractor.extract(accessor);

        ZetaUserDetails user = jwtAuthenticationService.authenticateAndGetDetails(jwtToken);
        accessor.setUser(new UsernamePasswordAuthenticationToken(user, jwtToken, user.getAuthorities()));
    }
}
