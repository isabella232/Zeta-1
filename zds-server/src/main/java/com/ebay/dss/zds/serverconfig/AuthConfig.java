package com.ebay.dss.zds.serverconfig;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

@EnableConfigurationProperties
@Configuration
@ConfigurationProperties(prefix = "auth")
public class AuthConfig {

    @NestedConfigurationProperty
    private AuthContext authContext = new AuthContext();
    @NestedConfigurationProperty
    private Jwt jwt = new Jwt();
    @NestedConfigurationProperty
    private EdgeProxy edgeProxy = new EdgeProxy();

    public AuthContext getAuthContext() {
        return authContext;
    }

    public void setAuthContext(AuthContext authContext) {
        this.authContext = authContext;
    }

    public EdgeProxy getEdgeProxy() {
        return edgeProxy;
    }

    public AuthConfig setEdgeProxy(EdgeProxy edgeProxy) {
        this.edgeProxy = edgeProxy;
        return this;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public AuthConfig setJwt(Jwt jwt) {
        this.jwt = jwt;
        return this;
    }

    public static class AuthContext {
        private String authName;
        private String authClass;

        public String getAuthName() {
            return authName;
        }

        public void setAuthName(String authName) {
            this.authName = authName;
        }

        public String getAuthClass() {
            return authClass;
        }

        public void setAuthClass(String authClass) {
            this.authClass = authClass;
        }
    }

    public static class Jwt {

        private int expirationTime;
        private String issuer;
        private String secret;
        private String ntHeader = "nt";
        private String authHeader = HttpHeaders.AUTHORIZATION;
        private String authCookie = "zeta-token";
        private String ntCookie = "zeta-nt";

        public String getNtHeader() {
            return ntHeader;
        }

        public Jwt setNtHeader(String ntHeader) {
            this.ntHeader = ntHeader;
            return this;
        }

        public String getNtCookie() {
            return ntCookie;
        }

        public Jwt setNtCookie(String ntCookie) {
            this.ntCookie = ntCookie;
            return this;
        }

        public String getAuthCookie() {
            return authCookie;
        }

        public Jwt setAuthCookie(String authCookie) {
            this.authCookie = authCookie;
            return this;
        }

        public String getAuthHeader() {
            return authHeader;
        }

        public Jwt setAuthHeader(String authHeader) {
            this.authHeader = authHeader;
            return this;
        }

        /**
         *
         * @return time in minutes
         */
        public int getExpirationTime() {
            return expirationTime;
        }

        public String getIssuer() {
            return issuer;
        }

        public Jwt setExpirationTime(int expirationTime) {
            this.expirationTime = expirationTime;
            return this;
        }

        public Jwt setIssuer(String issuer) {
            this.issuer = issuer;
            return this;
        }

        public Jwt setSecret(String secret) {
            this.secret = secret;
            return this;
        }

        public String getSecret() {
            return secret;
        }
    }

    public static class EdgeProxy {
        private boolean skip = false;
        private String url = "https://edgeproxy.dss.vip.ebay.com/auth-client-service/auth/ext/token";
        private String authHeader = HttpHeaders.AUTHORIZATION;

        public boolean isSkip() {
            return skip;
        }

        public EdgeProxy setSkip(boolean skip) {
            this.skip = skip;
            return this;
        }

        public String getUrl() {
            return url;
        }

        public EdgeProxy setUrl(String url) {
            this.url = url;
            return this;
        }

        public String getAuthHeader() {
            return authHeader;
        }

        public EdgeProxy setAuthHeader(String authHeader) {
            this.authHeader = authHeader;
            return this;
        }
    }
}
