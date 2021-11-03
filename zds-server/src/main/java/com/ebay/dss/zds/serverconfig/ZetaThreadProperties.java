package com.ebay.dss.zds.serverconfig;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "zds.threads")
public class ZetaThreadProperties {

    @NestedConfigurationProperty
    private Inbound inbound = new Inbound();
    @NestedConfigurationProperty
    private Broker broker = new Broker();
    @NestedConfigurationProperty
    private Outbound outbound = new Outbound();

    public Inbound getInbound() {
        return inbound;
    }

    public ZetaThreadProperties setInbound(Inbound inbound) {
        this.inbound = inbound;
        return this;
    }

    public Broker getBroker() {
        return broker;
    }

    public ZetaThreadProperties setBroker(Broker broker) {
        this.broker = broker;
        return this;
    }

    public Outbound getOutbound() {
        return outbound;
    }

    public ZetaThreadProperties setOutbound(Outbound outbound) {
        this.outbound = outbound;
        return this;
    }

    public static class Inbound extends ExecutorProperties {
    }

    public static class Broker extends ExecutorProperties {
    }

    public static class Outbound extends ExecutorProperties {
    }

    protected static class ExecutorProperties {
        private int core = 4;
        private int max = 8;
        private int queueCapacity = 128;
        private int keepAliveSeconds = 600;

        public int getCore() {
            return core;
        }

        public ExecutorProperties setCore(int core) {
            this.core = core;
            return this;
        }

        public int getMax() {
            return max;
        }

        public ExecutorProperties setMax(int max) {
            this.max = max;
            return this;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public ExecutorProperties setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
            return this;
        }

        public int getKeepAliveSeconds() {
            return keepAliveSeconds;
        }

        public void setKeepAliveSeconds(int keepAliveSeconds) {
            this.keepAliveSeconds = keepAliveSeconds;
        }

        @Override
        public String toString() {
            return "ExecutorProperties{" +
                    "core=" + core +
                    ", max=" + max +
                    ", queueCapacity=" + queueCapacity +
                    ", keepAliveSeconds=" + keepAliveSeconds +
                    '}';
        }
    }

};
