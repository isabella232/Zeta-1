package com.ebay.dss.zds.auth;

import com.ebay.dss.zds.model.ZetaUser;
import com.ebay.dss.zds.service.ZetaUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.WebAttributes;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Instant;

@RestController
public class LoginController {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    private final ZetaUserService zetaUserService;
    private final JwtTokenFactory tokenFactory;
    private final ZetaCookieFactory cookieFactory;

    @Autowired
    public LoginController(ZetaUserService zetaUserService,
                           JwtTokenFactory tokenFactory,
                           ZetaCookieFactory cookieFactory) {
        this.zetaUserService = zetaUserService;
        this.tokenFactory = tokenFactory;
        this.cookieFactory = cookieFactory;
    }

    @PostMapping("login")
    public void login() {
        // currently here should never be reached because
        // when login successful user will be redirected to token endpoint
        // or failed to error endpoint
    }

    @PostMapping("loginSuccess/token")
    public TokenResponse grantTokenOnSuccessLoginAndGenNtCookie(@AuthenticationPrincipal String nt, HttpServletResponse response) {
        log.info(nt + " is verified by provider, grant zeta token to it");

        ZetaUser user = zetaUserService.getOrCreateUser(nt);
        String jwtToken = tokenFactory.genTokenWithSameExpiration(user);

        response.addCookie(cookieFactory.genTokenCookie(jwtToken));
        response.addCookie(cookieFactory.genNtCookie(nt));

        return new TokenResponse(jwtToken);
    }


    @ModelAttribute(WebAttributes.AUTHENTICATION_EXCEPTION)
    public AuthenticationException getAuthEx(HttpServletRequest req) {
        return (AuthenticationException) req.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
    }

    @RequestMapping("loginFailure")
    public Object onIHubFailure(HttpServletResponse resp, @ModelAttribute(WebAttributes.AUTHENTICATION_EXCEPTION) AuthenticationException ex) {
        resp.setStatus(HttpStatus.UNAUTHORIZED.value());
        return new UnauthorizedResponse(HttpStatus.UNAUTHORIZED.value(), ex.getMessage(), "External authentication failed");
    }

    @RequestMapping("invalid")
    public Object onJwtFailure(HttpServletResponse resp, @ModelAttribute(WebAttributes.AUTHENTICATION_EXCEPTION) AuthenticationException ex) {
        resp.setStatus(HttpStatus.UNAUTHORIZED.value());
        return new UnauthorizedResponse(HttpStatus.UNAUTHORIZED.value(), ex.getMessage(), "Zeta token authentication failed");
    }

    static class TokenResponse {
        public final String token;

        TokenResponse(String token) {
            this.token = token;
        }
    }

    static class UnauthorizedResponse {

        public final long timestamp;
        public final int status;
        public final String error;
        public final String reason;

        public UnauthorizedResponse(int status, String reason, String error) {
            this.error = error;
            this.timestamp = Instant.now().getEpochSecond();
            this.status = status;
            this.reason = reason;
        }
    }
}
