package com.ebay.dss.zds.interpreter.monitor.common;

import com.ebay.dss.zds.interpreter.ConfigurationManager.Cluster;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

import static org.apache.hadoop.fs.CommonConfigurationKeysPublic.HADOOP_SECURITY_AUTHENTICATION;
import static org.apache.hadoop.fs.CommonConfigurationKeysPublic.HADOOP_SECURITY_AUTH_TO_LOCAL;

/**
 * Created by tatian on 2019-03-01.
 */
public class ClusterStatusFactory {

    private static final Logger logger = LoggerFactory.getLogger(ClusterStatusFactory.class);

    private Thread kinitThread;
    private String principal;
    private String keytab;
    private long kinitInterval;
    private static volatile boolean shouldKinit;
    private Properties prop;

    private static ClusterStatusFactory _clusterStatusFactory;
    public static final String HTTP = "http";
    public static final String RPC = "rpc";
    public static final String BDP = "bdp";
    public static final String DENGINE = "dengine";
    public static final String HERMES_VIEWPOINT = "hermes_viewpoint";
    public static final String HERMES_CONSOLE = "hermes_console";

    private ClusterStatusFactory(Properties prop) {

        this.prop = prop;

        this.shouldKinit = Boolean.valueOf(prop.getProperty("zds.monitor.kinit.open", "false"));

        if (shouldKinit) {
            this.principal = prop.getProperty("zds.principal", prop.getProperty("zds.livy.principal"));
            this.keytab = prop.getProperty("zds.keytab", prop.getProperty("zds.livy.keytab"));
            this.kinitInterval = Long.valueOf(prop.getProperty("zds.monitor.kinit.interval", "3600000"));

            Configuration hadoopConf = new Configuration();
            hadoopConf.set(HADOOP_SECURITY_AUTHENTICATION, "kerberos");
            hadoopConf.set(HADOOP_SECURITY_AUTH_TO_LOCAL, "RULE:[1:$1] RULE:[2:$1]");

            try {
                UserGroupInformation.setConfiguration(hadoopConf);
            } catch (Exception ex) {
                logger.error("Exception when set up ugi configuration: " + ex.toString());
            }
            UserGroupInformation ugi = null;
            try {
                ugi = UserGroupInformation.getCurrentUser();
            } catch (IOException ex) {
                logger.info("failed to get current ugi!");
            }

            logger.info("ready to set up ugi, the current user is: " + ugi);
            doKinit();
            logger.info("after kinit, the current user is: " + ugi);

            kinitThread = new Thread(() -> {
                while (shouldKinit) {

                    doKinit();

                    try {
                        Thread.sleep(kinitInterval);
                    } catch (Exception ex) {
                        logger.error(ex.toString());
                        shouldKinit = false;
                    }
                }
            });
            kinitThread.start();
            logger.info("kinit thread started");
        }
    }

    public static synchronized final ClusterStatusFactory getInstance(Properties prop) {
        if (_clusterStatusFactory == null) {
            _clusterStatusFactory = new ClusterStatusFactory(prop);
        }
        return _clusterStatusFactory;
    }

    private void doKinit() {
        try {
            logger.info("do kinit: {} {}", principal, keytab);
            UserGroupInformation.loginUserFromKeytab(principal, keytab);
            logger.info("kinit done!");
        } catch (Exception ex) {
            logger.error("kinit thread failed: " + ex.toString());
            logger.error("kinit thread exited");
            shouldKinit = false;
        }
    }

    public ClusterStatus getClusterStatus(Properties prop, Cluster cluster, String type) {
       return create(prop, cluster, type);
    }

    public static ClusterStatus create(Properties prop, Cluster cluster, String type) {

        switch (type) {
            case BDP:
                return new ClusterStatusBDPRESTImpl(cluster);
            case HTTP:
                return new ClusterStatusYarnRESTImpl(YarnUrl.get(cluster));
            case DENGINE:
                return new ClusterStatusDengineRESTImpl(prop.getProperty("zds.dengine.url") , cluster);
            default:
                return new ClusterStatusYarnRESTImpl(YarnUrl.get(cluster));
        }
    }

}
