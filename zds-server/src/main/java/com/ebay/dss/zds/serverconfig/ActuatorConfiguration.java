package com.ebay.dss.zds.serverconfig;

import com.ebay.dss.zds.message.endpoint.InterpreterActuatorEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by tatian on 2019-06-19.
 */

@Configuration
public class ActuatorConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActuatorConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnAvailableEndpoint
    public InterpreterActuatorEndpoint interpreterEndpoint() {
        LOGGER.info("Init interpreter actuator endpoint");
        return new InterpreterActuatorEndpoint();
    }
}
