package com.ebay.dss.zds.serverconfig;

import com.ebay.dss.zds.websocket.AuthenticationInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

/**
 * Created by wenliu2 on 4/4/18.
 */
@Configuration
@EnableWebSocketMessageBroker
// this order is important leading to register our security interceptor adapter
// in front of all channel interceptor
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    protected static final Logger logger = LoggerFactory.getLogger(WebSocketConfig.class);

    private final AuthenticationInterceptor authenticationInterceptor;

    private final ZetaThreadProperties properties;

    @Autowired
    public WebSocketConfig(AuthenticationInterceptor authenticationInterceptor,
                           ZetaThreadProperties properties) {
        this.authenticationInterceptor = authenticationInterceptor;
        this.properties = properties;
    }

    private static ThreadPoolTaskExecutor configureExecutor(ZetaThreadProperties.ExecutorProperties properties) {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(properties.getCore());
        taskExecutor.setMaxPoolSize(properties.getMax());
        if (properties.getQueueCapacity() > 0) {
            taskExecutor.setQueueCapacity(properties.getQueueCapacity());
        }
        taskExecutor.setKeepAliveSeconds(properties.getKeepAliveSeconds());
        logger.info("Thread pool configured, " + properties);
        return taskExecutor;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("*")
                .withSockJS();

    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        logger.info("Configure Message Broker");
        // publish the message in order
        config.setPreservePublishOrder(true);

        ThreadPoolTaskExecutor taskExecutor = configureExecutor(properties.getBroker());
        config.configureBrokerChannel()
                .taskExecutor(taskExecutor);
        ServerConfigurationContext.webSocketMessageBrokerExecutor = taskExecutor;


        ThreadPoolTaskScheduler te = new ThreadPoolTaskScheduler();
        te.setPoolSize(10);
        te.setThreadNamePrefix("wss-heartbeat-thread-");
        te.initialize();

        config.setApplicationDestinationPrefixes("/wsapp");
        config.enableSimpleBroker("/topic", "/queue")
                .setTaskScheduler(te)
                .setHeartbeatValue(new long[]{10000L, 30000L});
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        registry.setMessageSizeLimit(17 * 2<<20);
        registry.setSendBufferSizeLimit(1024 * 1024);
        registry.setSendTimeLimit(120 * 1000);
        registry.setTimeToFirstMessage(120 * 1000);
    }

    @Override
    public void configureClientInboundChannel(final ChannelRegistration registration) {
        logger.info("Configure Inbound channel");
        registration.interceptors(authenticationInterceptor);

        ThreadPoolTaskExecutor taskExecutor = configureExecutor(properties.getInbound());
        registration.taskExecutor(taskExecutor);
        ServerConfigurationContext.webSocketInboundExecutor = taskExecutor;

        //registration.interceptors(new SessionCloseInterceptorAdapter());
    }

    /**
     * Configure the {@link org.springframework.messaging.MessageChannel} used
     * for outgoing messages to WebSocket clients. By default the channel is
     * backed by a thread pool of size 1. It is recommended to customize thread
     * pool settings for production use.
     */
    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        logger.info("Configure Outbound channel");
        ThreadPoolTaskExecutor taskExecutor = configureExecutor(properties.getOutbound());
        registration.taskExecutor(taskExecutor);

        ServerConfigurationContext.webSocketOutboundExecutor = taskExecutor;
    }


}
