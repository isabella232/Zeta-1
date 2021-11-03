package com.ebay.dss.zds.kerberos;

import org.apache.hadoop.security.UserGroupInformation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TicketCacheKerberosContext extends KerberosContext {

    private static final Logger logger = LogManager.getLogger();
    private String ticketCacheLocation;
    private String principal;

    TicketCacheKerberosContext(UserGroupInformation ugi) {
        super(ugi);
    }

    public TicketCacheKerberosContext(String principal, String ticketCacheLocation) {
        this.principal = principal;
        this.ticketCacheLocation = ticketCacheLocation;
    }

    @Override
    protected void loginRefresh() throws Exception {
        login();
    }

    @Override
    protected void login() throws Exception {
        UserGroupInformation.setConfiguration(defaultConfiguration());
        ugi = UserGroupInformation.
                getUGIFromTicketCache(ticketCacheLocation, principal);
    }

    @Override
    public String toString() {
        return "TicketCacheKerberosContext{" +
                "ticketCacheLocation='" + ticketCacheLocation + '\'' +
                ", principal='" + principal + '\'' +
                ", refreshIntervalScale=" + refreshIntervalScale +
                ", refreshIntervalUnit=" + refreshIntervalUnit +
                ", loginCnt=" + loginCnt +
                ", closed=" + closed +
                "} " + super.toString();
    }

    @Override
    protected void loginError() throws Exception {
    }

}
