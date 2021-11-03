package com.ebay.dss.zds.kerberos;

import org.apache.hadoop.security.UserGroupInformation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class KeytabKerberosContext extends KerberosContext {

    private static final Logger logger = LogManager.getLogger();

    KeytabKerberosContext(UserGroupInformation ugi) {
        super(ugi);
    }

    private String keytabLocation;
    private String principal;

    public KeytabKerberosContext(String principal, String keytabLocation) {
        this.principal = principal;
        this.keytabLocation = keytabLocation;
        setRefreshIntervalScale(10);
        setRefreshIntervalUnit(TimeUnit.MINUTES);
    }

    @Override
    protected void loginRefresh() throws IOException {
        ugi.reloginFromKeytab();
    }

    @Override
    protected void login() throws IOException {
        UserGroupInformation.setConfiguration(defaultConfiguration());
        ugi = UserGroupInformation.
                loginUserFromKeytabAndReturnUGI(principal, keytabLocation);
    }

    @Override
    protected void loginError() {
    }

    @Override
    public String toString() {
        return "KeytabKerberosContext{" +
                "keytabLocation='" + keytabLocation + '\'' +
                ", principal='" + principal + '\'' +
                ", closed=" + closed +
                '}';
    }
}
