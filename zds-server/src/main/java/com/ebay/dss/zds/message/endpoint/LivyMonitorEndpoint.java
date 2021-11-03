package com.ebay.dss.zds.message.endpoint;

import com.ebay.dss.zds.interpreter.ConfigurationManager;
import com.ebay.dss.zds.interpreter.InterpreterConfiguration;
import com.ebay.dss.zds.interpreter.interpreters.livy.LivyClient;
import com.ebay.dss.zds.interpreter.interpreters.livy.LivyClientBuilder;
import org.apache.zeppelin.livy.LivyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by tatian on 2019-07-22.
 */
@Component
public class LivyMonitorEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(PrometheusMetricRegistry.class);

    final ConfigurationManager configurationManager;
    private Environment env;

    private Map<Integer, LivyClient> livyClients;

    public LivyMonitorEndpoint(ConfigurationManager configurationManager, Environment env) {
        this.configurationManager = configurationManager;
        this.env = env;
    }

    @PostConstruct
    public void init() {
        livyClients = new HashMap<>();
        InterpreterConfiguration conf = configurationManager.getDefaultConfiguration();

        if (env.acceptsProfiles(Profiles.of("dev"))) {
            return;
        }

        try {
            // areslvs
            livyClients.put(2, buildClient(2, conf));

            // herculeslvs
            livyClients.put(10, buildClient(10, conf));

            // herculessublvs
            livyClients.put(11, buildClient(11, conf));

            // apollorno
            livyClients.put(14, buildClient(14, conf));
        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }
    }

    private LivyClient buildClient(int clusterId, InterpreterConfiguration conf) {
        return new LivyClientBuilder().withInterpreterConfiguration(conf).withClusterId(clusterId, false).build();
    }

    public Integer isLivyAlive(int clusterId) {
        if (env.acceptsProfiles(Profiles.of("dev"))) {
            return 1;
        }
        LivyClient livyClient = livyClients.get(clusterId);
        if (livyClient != null) {
            try {
                livyClient.getLivyVersion();
                return 1;
            } catch (LivyException ex) {
                return 0;
            }
        } else {
            return 0;
        }
    }
}
