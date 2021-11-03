package com.ebay.dss.zds.serverconfig;

import com.ebay.dss.zds.service.ElasticLogJobs;
import com.ebay.dss.zds.service.ElasticLogService;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;

@Configuration
public class ElasticConfig {

    private static final Logger logger = LogManager.getLogger();

    private final ElasticClusterProperties clusterProperties;

    public ElasticConfig(ElasticClusterProperties clusterProperties) {
        this.clusterProperties = clusterProperties;
    }

    HttpHost[] resolveHttpHosts(String[] addresses) {
        List<HttpHost> httpHosts = new ArrayList<>();
        for (String address : addresses) {
            String schema;
            String host;
            int port;

            int schemaIndex = StringUtils.indexOf(address, "://");
            if (schemaIndex > -1) {
                schema = StringUtils.substring(address, 0, schemaIndex);
                address = StringUtils.substring(address, schemaIndex + 3, address.length());
            } else {
                schema = "http";
            }

            int portIndex = StringUtils.indexOf(address, ':');

            if (portIndex > -1) {
                host = StringUtils.substring(address, 0, portIndex);
                port = Integer.parseInt(StringUtils.substring(address, portIndex + 1));
            } else {
                host = StringUtils.substring(address, 0);
                port = StringUtils.equals(schema, "http") ? 80 : 443;
            }

            httpHosts.add(new HttpHost(host, port, schema));
        }
        return httpHosts.toArray(new HttpHost[0]);
    }

    @Bean
    public RestHighLevelClient client() {
        try {
            HttpHost[] httpHosts = resolveHttpHosts(clusterProperties.getAddresses());
            RestClientBuilder restClientBuilder = RestClient.builder(httpHosts);
            if (StringUtils.isNotBlank(clusterProperties.pathPrefix)) {
                restClientBuilder.setPathPrefix(clusterProperties.getPathPrefix());
            }
            if (StringUtils.isNoneBlank(clusterProperties.getUsername(), clusterProperties.getPassword())) {
                final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(AuthScope.ANY,
                        new UsernamePasswordCredentials(
                                clusterProperties.getUsername(),
                                clusterProperties.getPassword()));
                restClientBuilder.setHttpClientConfigCallback(httpClientBuilder ->
                        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
            }
            return new RestHighLevelClient(restClientBuilder);
        } catch (Exception e) {
            throw new RuntimeException("Can not find host or port from addresses " + Arrays.toString(clusterProperties.addresses));
        }
    }

    @Bean(name = "ElasticLogTaskExecutor")
    public Executor elasticTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setThreadNamePrefix("Elastic-Log-");
        executor.initialize();
        return executor;
    }

    @Bean
    @ConditionalOnProperty(value = "zds.elastic.log.schedule.enabled", havingValue = "true")
    @Autowired
    public ElasticLogJobs aceSearchLogJob(ElasticLogService service) {
        return new ElasticLogJobs(service);
    }

    @Component
    @ConfigurationProperties(prefix = "zds.elastic.cluster")
    public static class ElasticClusterProperties {
        private String[] addresses = {"localhost:9200"};
        private String name;
        private String pathPrefix;
        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public ElasticClusterProperties setUsername(String username) {
            this.username = username;
            return this;
        }

        public String getPassword() {
            return password;
        }

        public ElasticClusterProperties setPassword(String password) {
            this.password = password;
            return this;
        }

        public String getPathPrefix() {
            return pathPrefix;
        }

        public ElasticClusterProperties setPathPrefix(String pathPrefix) {
            this.pathPrefix = pathPrefix;
            return this;
        }

        public String[] getAddresses() {
            return addresses;
        }

        public ElasticClusterProperties setAddresses(String[] addresses) {
            this.addresses = addresses;
            return this;
        }

        public String getName() {
            return name;
        }

        public ElasticClusterProperties setName(String name) {
            this.name = name;
            return this;
        }
    }

    @Component
    @ConfigurationProperties(prefix = "zds.elastic.log.ace")
    public static class ElasticLogAceProperties {
        private Boolean enabled = false;
        private String indexPrefix = "log-ace-history-default";

        public Boolean getEnabled() {
            return enabled;
        }

        public ElasticLogAceProperties setEnabled(Boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public String getIndexPrefix() {
            return indexPrefix;
        }

        public ElasticLogAceProperties setIndexPrefix(String indexPrefix) {
            this.indexPrefix = indexPrefix;
            return this;
        }
    }

    @Component
    @ConfigurationProperties(prefix = "zds.elastic.log")
    public static class ElasticLogProperties {
        private Long bulkIdleTimeout = 10 * 60 * 1000L;
        private Long bulkSize = 100L;
        private Long bulkTimeout = 10 * 1000L;
        private Boolean enabled = false;

        public Long getBulkIdleTimeout() {
            return bulkIdleTimeout;
        }

        public ElasticLogProperties setBulkIdleTimeout(Long bulkIdleTimeout) {
            this.bulkIdleTimeout = bulkIdleTimeout;
            return this;
        }

        public Long getBulkSize() {
            return bulkSize;
        }

        public ElasticLogProperties setBulkSize(Long bulkSize) {
            this.bulkSize = bulkSize;
            return this;
        }

        public Long getBulkTimeout() {
            return bulkTimeout;
        }

        public ElasticLogProperties setBulkTimeout(Long bulkTimeout) {
            this.bulkTimeout = bulkTimeout;
            return this;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public ElasticLogProperties setEnabled(Boolean enabled) {
            this.enabled = enabled;
            return this;
        }

    }

    @Component("elastic-log-schedule-properties")
    @ConfigurationProperties(prefix = "zds.elastic.log.schedule")
    public static class ElasticLogScheduleProperties {
        private Long fixedDelay = 5 * 1000L;
        private Long fixedRate = 5 * 1000L;
        private Boolean enabled = false;

        public Long getFixedDelay() {
            return fixedDelay;
        }

        public ElasticLogScheduleProperties setFixedDelay(Long fixedDelay) {
            this.fixedDelay = fixedDelay;
            return this;
        }

        public Long getFixedRate() {
            return fixedRate;
        }

        public ElasticLogScheduleProperties setFixedRate(Long fixedRate) {
            this.fixedRate = fixedRate;
            return this;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public ElasticLogScheduleProperties setEnabled(Boolean enabled) {
            this.enabled = enabled;
            return this;
        }
    }

}
