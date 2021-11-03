package com.ebay.dss.zds.kerberos;

import org.apache.hadoop.security.UserGroupInformation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.security.auth.Subject;
import javax.security.auth.callback.*;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.io.IOException;

public class UsernamePasswordKerberosContext extends KerberosContext implements CallbackHandler {

    private static final Logger logger = LogManager.getLogger();

    private String username;
    private char[] password;
    private LoginContext loginContext;
    private Subject subject;

    public UsernamePasswordKerberosContext(String username, char[] password) {
        this.username = username;
        this.password = password;
    }

    private static final String JAAS_CONFIG_NAME = "KerberosDefault";

    public UsernamePasswordKerberosContext(UserGroupInformation ugi) {
        super(ugi);
    }

    @Override
    protected void loginRefresh() throws Exception {
        login();
    }

    @Override
    protected void login() throws Exception {
        configureUgi();
        loginContext = getLoginContext();
        loginContext.login();
        subject = loginContext.getSubject();
        ugi = getUgi(subject);
    }

    void configureUgi() {
        UserGroupInformation.setConfiguration(defaultConfiguration());
    }

    LoginContext getLoginContext() throws LoginException {
        return new LoginContext(JAAS_CONFIG_NAME, this);
    }

    UserGroupInformation getUgi(Subject subject) throws IOException {
        return UserGroupInformation.getUGIFromSubject(subject);
    }

    @Override
    protected void loginError() {
    }

    @Override
    public void handle(Callback[] callbacks) throws UnsupportedCallbackException {
        for (Callback callback : callbacks) {
            if (callback instanceof NameCallback) {
                NameCallback nc = (NameCallback) callback;
                nc.setName(username);
            } else if (callback instanceof PasswordCallback) {
                PasswordCallback pc = (PasswordCallback) callback;
                pc.setPassword(password);
            } else if (callback instanceof TextOutputCallback) {
                TextOutputCallback tc = (TextOutputCallback) callback;

                switch (tc.getMessageType()) {
                    case TextOutputCallback.INFORMATION:
                        logger.info(tc.getMessage());
                        break;
                    case TextOutputCallback.WARNING:
                        logger.warn(tc.getMessage());
                        break;
                    case TextOutputCallback.ERROR:
                        logger.error(tc.getMessage());
                        break;
                    default:
                        throw new UnsupportedCallbackException(
                                callback, "Unrecognized message type");
                }
            } else {
                throw new UnsupportedCallbackException(
                        callback, "Unrecognized Callback");
            }
        }
    }

    @Override
    public String toString() {
        return "UsernamePasswordKerberosContext{" +
                "username='" + username + '\'' +
                ", closed=" + closed +
                '}';
    }
}
