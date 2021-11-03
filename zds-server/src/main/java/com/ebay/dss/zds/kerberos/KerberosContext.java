package com.ebay.dss.zds.kerberos;

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.security.PrivilegedExceptionAction;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class KerberosContext {

    private static final Logger logger = LogManager.getLogger();

    protected UserGroupInformation ugi;
    protected long refreshIntervalScale = 5;
    protected TimeUnit refreshIntervalUnit = TimeUnit.MINUTES;
    protected AtomicInteger loginCnt = new AtomicInteger();
    protected boolean closed = true;

    public KerberosContext() {
    }

    public KerberosContext(UserGroupInformation ugi) {
        this.ugi = ugi;
    }

    public long getRefreshIntervalScale() {
        return refreshIntervalScale;
    }

    public void setRefreshIntervalScale(long refreshIntervalScale) {
        this.refreshIntervalScale = refreshIntervalScale;
    }

    public TimeUnit getRefreshIntervalUnit() {
        return refreshIntervalUnit;
    }

    public void setRefreshIntervalUnit(TimeUnit refreshIntervalUnit) {
        this.refreshIntervalUnit = refreshIntervalUnit;
    }

    public void doKerberosLogin() throws Exception {
        try {
            if (Objects.nonNull(ugi)) {
                logger.info("found exists user group information, do relogin for {}", this);
                loginRefresh();
            } else {
                logger.info("not found exists user group information, do creation for {}", this);
                login();
            }
            closed = false;
            loginCnt.incrementAndGet();
            logger.info("login done for {}, this is {} time(s)", this, loginCnt.get());
        } catch (Exception e) {
            loginError();
            logger.error("failed to login for {}", this);
            closed = true;
            throw e;
        }
    }

    public boolean isClosed() {
        return closed;
    }

    protected abstract void loginRefresh() throws Exception;

    protected abstract void login() throws Exception;

    protected abstract void loginError() throws Exception;

    public void close() {
        this.closed = true;
    }

    @InterfaceStability.Evolving
    @InterfaceAudience.Public
    public <T> T doAs(PrivilegedExceptionAction<T> action) throws Exception {
        if (Objects.isNull(ugi) || isClosed()) {
            throw new IOException("Kerberos login is dead, please create a new context");
        }
        return ugi.doAs(action);
    }

    static Configuration defaultConfiguration() {
        Configuration conf = new Configuration();
        conf.set(CommonConfigurationKeysPublic.HADOOP_SECURITY_AUTHENTICATION, "KERBEROS");
        conf.set(CommonConfigurationKeysPublic.HADOOP_SECURITY_AUTH_TO_LOCAL, "RULE:[1:$1]\nRULE:[2:$1]\nDEFAULT");
        return conf;
    }
}
