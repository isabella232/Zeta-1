package com.ebay.dss.zds.interpreter.monitor.common;

import com.ebay.dss.zds.common.JsonUtil;
import com.ebay.dss.zds.interpreter.InterpreterConfiguration;
import com.ebay.dss.zds.interpreter.monitor.modle.JobDTO;
import com.ebay.dss.zds.interpreter.monitor.modle.YarnQueue;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by tatian on 2018/5/25.
 */
public class YarnStatusHTTPClient implements YarnStatusClient {

    protected static final Logger logger = LoggerFactory.getLogger(YarnStatusHTTPClient.class);

    private CloseableHttpClient httpClient;

    private static YarnStatusHTTPClient yarnAppHTTPClient;

    private static final String propertyKey="zds.monitor.yarn.client.httpPool.size";

    public static YarnStatusHTTPClient create() {
        if (yarnAppHTTPClient == null) {
            yarnAppHTTPClient = new YarnStatusHTTPClient();
        }
        return yarnAppHTTPClient;
    }

    public static YarnStatusHTTPClient create(int poolSize) {
        if (yarnAppHTTPClient == null) {
            yarnAppHTTPClient = new YarnStatusHTTPClient(poolSize);
        }
        return yarnAppHTTPClient;
    }

    private YarnStatusHTTPClient() {
        init(Integer.valueOf(InterpreterConfiguration
                .loadFromDefaultPath()
                .getProperties()
                .getOrDefault(propertyKey,"500")
                .toString()));
    }

    private YarnStatusHTTPClient(Integer poolSize) {
        init(poolSize);
    }

    public void init(Integer poolSize) {
        SSLConnectionSocketFactory sslsf = null;
        PoolingHttpClientConnectionManager cm = null;
        SSLContextBuilder builder = null;
        try {
            builder = new SSLContextBuilder();
            //trust all
            builder.loadTrustMaterial(null, new TrustStrategy() {
                @Override
                public boolean isTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                    return true;
                }
            });
            sslsf = new SSLConnectionSocketFactory(builder.build(), new String[]{"SSLv2Hello", "SSLv3", "TLSv1", "TLSv1.2"}, null, NoopHostnameVerifier.INSTANCE);
            Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", new PlainConnectionSocketFactory())
                    .register("https", sslsf)
                    .build();
            cm = new PoolingHttpClientConnectionManager(registry);
            cm.setMaxTotal(poolSize);//max connection
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.httpClient = HttpClients.custom()
                .setSSLSocketFactory(sslsf)
                .setConnectionManager(cm)
                .setConnectionManagerShared(true)
                .build();
        logger.info("Yarn Client inited");
    }

    public String httpGet(String url) {
        String content = null;
        HttpGet get = new HttpGet(url);
        RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(10000)
                .setConnectTimeout(10000)
                .setConnectionRequestTimeout(10000)
                .build();
        get.setConfig(requestConfig);
        try {
            CloseableHttpResponse httpResponse = httpClient.execute(get);
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                content = EntityUtils.toString(httpResponse.getEntity());
                //logger.debug("Content of job into: "+content);
            } else {
                logger.info("Failed to get: " + url);
            }
        } catch (IOException ex) {
            logger.error(ex.getMessage());
        }finally {
            get.releaseConnection();
        }
        return content;
    }

    public List<JobDTO> getJobs(String url) {
        List<JobDTO> jobs = null;
        try {
            jobs = parseJob(httpGet(url));
            if (jobs == null) {
                logger.info("Failed to get job info!");
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }
        return jobs;
    }

    public List<YarnQueue> getQueues(String url) {
        List<YarnQueue> queues = null;
        try {
            queues = parseQueue(httpGet(url));
            if (queues == null) {
                logger.info("Failed to get queue info!");
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }
        return queues;
    }

    public YarnQueue getQueue(String name){
        throw new UnsupportedOperationException("");
    }

    public static List<YarnQueue> parseQueue(String content) {
        List<YarnQueue> queues = null;
        try {
            if (content == null) return queues;
            JsonObject base = JsonUtil.jsonParser
                    .parse(content)
                    .getAsJsonObject();
            if (base.has("scheduler")) {
                queues = JsonUtil.GSON.fromJson(base
                        .getAsJsonObject("scheduler")
                        .getAsJsonObject("schedulerInfo")
                        .getAsJsonObject("queues")
                        .getAsJsonArray("queue").toString(), new TypeToken<List<YarnQueue>>() {
                }.getType());
            } else if (base.has("queueName")) {
                YarnQueue queue = JsonUtil.GSON.fromJson(base.toString(), new TypeToken<YarnQueue>() {}.getType());
                queues = new ArrayList<>();
                queues.add(queue);
            }
        } catch (Exception ex) {
            logger.error("Failed to parse yarn queues");
        }
        return queues;
    }

    public static List<JobDTO> parseJob(String content) {
        return content != null ? JsonUtil.GSON.fromJson(content, new TypeToken<List<JobDTO>>() {
        }.getType()) : null;
    }

    public void close() {
        try {
            this.httpClient.close();
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }
}
