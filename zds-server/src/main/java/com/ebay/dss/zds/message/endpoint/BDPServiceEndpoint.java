package com.ebay.dss.zds.message.endpoint;

import com.ebay.dss.zds.interpreter.monitor.modle.YarnQueue;
import com.ebay.dss.zds.service.BDPHTTPService;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * Created by tatian on 2019-06-21.
 */
@Component
public class BDPServiceEndpoint {

    private final BDPHTTPService bdphttpService;
    private final Environment env;

    public BDPServiceEndpoint(BDPHTTPService bdphttpService, Environment env) {
        this.bdphttpService = bdphttpService;
        this.env = env;
    }

    public double getConnectionQueueUsedPct(String clusterName) {
        if (env.acceptsProfiles(Profiles.of("dev"))) {
            return 1;
        }
        YarnQueue yarnQueue = bdphttpService.getZetaConnectionQueue(clusterName);
        return yarnQueue == null ? -1 : yarnQueue.getUsedPct();
    }
}
