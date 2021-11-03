package com.ebay.dss.zds.auth;

import com.ebay.dss.zds.common.LogUtil;
import com.ebay.dss.zds.model.ZetaUser;
import com.ebay.dss.zds.serverconfig.AuthConfig;
import com.ebay.dss.zds.service.ZetaUserService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.impl.DefaultClaims;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class JwtTokenFactory {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenFactory.class);
    private static final String JWT_PROPERTY_ROLE = "roles";
    private final AuthConfig.Jwt config;
    private final ZetaUserService userService;

    @Autowired
    public JwtTokenFactory(AuthConfig authConfig, ZetaUserService userService) {
        this.config = authConfig.getJwt();
        this.userService = userService;
    }

    public String jwtToken(ZetaUser user) {
        Instant now = Instant.now();
        return jwtToken(now, user, Date.from(now.plus(config.getExpirationTime(), ChronoUnit.MINUTES)));
    }

    public String jwtToken(ZetaUser user, Date expiration) {
        Instant now = Instant.now();
        return jwtToken(now, user, expiration);
    }

    public String jwtToken(Instant baseTime, ZetaUser user, Date expiration) {
        if (Objects.isNull(user)) {
            throw new NullPointerException("Cannot generate token for null user.");
        }
        return Jwts.builder()
                .setClaims(privateClaims(user))
                .setId(user.getNt() + baseTime.toEpochMilli())
                .setSubject(user.getNt())
                .setIssuer(config.getIssuer())
                .setIssuedAt(Date.from(baseTime))
                .setExpiration(expiration)
                .signWith(SignatureAlgorithm.HS512, config.getSecret())
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
                .compact();
    }

    public String genTokenWithSameExpiration(ZetaUser user) {
        ZetaUserDetails verifiedTokenUserDetail = null;
        try {
            verifiedTokenUserDetail = verify(user.getToken());
        } catch (ExpiredJwtException e) {
            log.info("Jwt token expired, generate new one");
        } catch (IllegalArgumentException e) {
            if (StringUtils.contains(e.getMessage(), "null or empty")) {
                log.info("Jwt token is null or empty, generate new one");
            } else {
                log.warn("Jwt token error", e);
            }
        } catch (Exception e) {
            log.warn("Jwt token error", e);
        } finally {
            if (verifiedTokenUserDetail == null) {
                // this token works as base token to provide some base information before it self expired
                // currently it only provides expiration time to actual user token;
                String baseToken = genAndSaveToken(user);
                verifiedTokenUserDetail = verify(baseToken);
            }
        }
        String token = jwtToken(user, verifiedTokenUserDetail.getExpiration());
        log.info(user.getNt() + "'s zeta token generated: " + LogUtil.displayableSecret(token, 5, 5, 50));
        return token;
    }

    String genAndSaveToken(ZetaUser user) {
        String token = jwtToken(user);
        userService.updateToken(user.getNt(), token);
        log.info(user.getNt() + "'s zeta base token generated: " + LogUtil.displayableSecret(token, 5, 5, 50));
        return token;
    }


    private Claims privateClaims(ZetaUser user) {
        Claims claims = new DefaultClaims();
        claims.put(JWT_PROPERTY_ROLE, getRoleString(user));
        return claims;
    }

    private String getRoleString(ZetaUser user) {
        List<String> roles = new ArrayList<>();
        roles.add("user");
        if (user.getAdmin() > 0) {
            roles.add("admin");
        }
        if (user.isAceAdmin()) {
            roles.add("ace_admin");
        }
        return StringUtils.join(roles, ",");
    }

    public ZetaUserDetails verify(String jwtToken) {
        Jws<Claims> claims = Jwts.parser()
                .setSigningKey(config.getSecret())
                .parseClaimsJws(jwtToken);
        String name = claims.getBody().getSubject();
        String roles = (String) claims.getBody().getOrDefault(JWT_PROPERTY_ROLE, "user");
        List<GrantedAuthority> authorities = Arrays.asList(roles.split(",", -1)).stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .collect(Collectors.toList());
        return new ZetaUserDetails(name, authorities, claims.getBody().getExpiration());
    }

}
