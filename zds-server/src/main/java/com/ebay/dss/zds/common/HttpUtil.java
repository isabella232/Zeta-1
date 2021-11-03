package com.ebay.dss.zds.common;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.codehaus.plexus.util.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

/**
 * Created by tatian on 2020-03-17.
 */
public class HttpUtil {

  protected static final Logger logger = LoggerFactory.getLogger(HttpUtil.class);

  // private static CloseableHttpClient httpClient = forceGetHttpClient();

  private static CloseableHttpClient forceGetHttpClient() {
    try {
      return getHttpClient();
    } catch (Exception ex) {
      return null;
    }
  }

  public static HttpClientBuilder getHttpClientBuilder() throws
          KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
    SSLContextBuilder builder = new SSLContextBuilder();
    builder.loadTrustMaterial(null, (X509Certificate[] certs, String s) -> true);
    SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build(),
            new String[]{"SSLv2Hello", "SSLv3", "TLSv1", "TLSv1.2"},
            null, NoopHostnameVerifier.INSTANCE);
    return HttpClients.custom()
            .evictExpiredConnections()
            .evictIdleConnections(30, TimeUnit.SECONDS)
            .setSSLSocketFactory(sslsf);
  }

  public static CredentialsProvider createCredentialsProvider(String proxyHost,
                                                              int proxyPort,
                                                              String proxyUser,
                                                              String proxyPassword) {
    CredentialsProvider credentialProvider = new CredentialsProvider() {

      private AuthScope scope;
      private Credentials credentials;

      public void setCredentials(AuthScope scope, Credentials credentials) {
        this.scope = scope;
        this.credentials = credentials;
      }

      public Credentials getCredentials(AuthScope scope) {
        return this.credentials;
      }

      public void clear() {
        this.scope = null;
        this.credentials = null;
      }
    };

    credentialProvider.setCredentials(new AuthScope(proxyHost, proxyPort),
            new UsernamePasswordCredentials(proxyUser, proxyPassword));

    return credentialProvider;
  }

  public static HttpClientBuilder setupProxy(HttpClientBuilder builder,
                                             String proxyHost,
                                             int proxyPort,
                                             String scheme,
                                             String proxyUser,
                                             String proxyPassword) {
    HttpHost proxy = HttpHost.create(String.format("%s://%s:%s", scheme, proxyHost, proxyPort));
    CredentialsProvider credentialProvider = createCredentialsProvider(proxyHost, proxyPort, proxyUser, proxyPassword);
    builder.setProxy(proxy);
    builder.setDefaultCredentialsProvider(credentialProvider);
    return builder;
  }

  public static CloseableHttpClient getHttpClient() throws
          KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
    return getHttpClientBuilder().build();
  }

  public static CloseableHttpClient getHttpClient(int poolSize) throws
          KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
    SSLContextBuilder builder = new SSLContextBuilder();
    builder.loadTrustMaterial(null, (X509Certificate[] certs, String s) -> true);
    SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build(),
            new String[]{"SSLv2Hello", "SSLv3", "TLSv1", "TLSv1.2"},
            null, NoopHostnameVerifier.INSTANCE);
    Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
            .register("http", PlainConnectionSocketFactory.getSocketFactory())
            .register("https", sslsf)
            .build();
    PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(registry);
    connectionManager.setMaxTotal(poolSize);
    connectionManager.setDefaultMaxPerRoute(poolSize);

    return HttpClients.custom()
            .setConnectionManager(connectionManager)
            .evictExpiredConnections()
            .evictIdleConnections(30, TimeUnit.SECONDS)
            .build();
  }

  public static HttpGet httpGet(String url) {
    HttpGet httpGet = new HttpGet(url);
    //httpGet.setHeader("Content-Type", "application/json;charset=UTF-8");
    httpGet.setHeader("X-Requested-By", "zds");
    httpGet.setHeader("Connection", "close");
    return httpGet;
  }

  public static HttpPost httpPost(String url, String json) throws Exception {
    HttpPost httpPost = new HttpPost(url);
    httpPost.setEntity(new StringEntity(json));
    httpPost.setHeader("Content-Type", "application/json;charset=UTF-8");
    httpPost.setHeader("X-Requested-By", "zds");
    httpPost.setHeader("Connection", "close");
    return httpPost;
  }

  public static HttpDelete httpDelete(String url) throws Exception {
    HttpDelete httpDelete = new HttpDelete(url);
    httpDelete.setHeader("Content-Type", "application/json;charset=UTF-8");
    httpDelete.setHeader("X-Requested-By", "zds");
    httpDelete.setHeader("Connection", "close");
    return httpDelete;
  }

  public static String response2String(CloseableHttpResponse response) throws IOException {
    return EntityUtils.toString(response.getEntity());
  }

  public static String httpGetResource(String url) {
    return httpGetResource(url, 10000);
  }

  public static String httpGetResource(String url, int conn_timeout) {
    return httpGetResource(httpGet(url), conn_timeout);
  }

  public static String httpGetResource(HttpGet httpget, int conn_timeout) {
    CloseableHttpClient client = null;
    try {
      client = forceGetHttpClient();
      return httpGetResource(client, httpget, conn_timeout);
    } catch (Exception ex) {
      return null;
    } finally {
      if (client != null) {
        try {
          client.close();
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      }
    }
  }

  public static String httpGetResource(CloseableHttpClient closeableHttpClient, String url, int conn_timeout) {
    return httpGetResource(closeableHttpClient, httpGet(url), conn_timeout);
  }

  public static String httpGetResource(CloseableHttpClient closeableHttpClient, HttpGet httpget, int conn_timeout) {
    String url = httpget.getURI().toString();
    logger.debug("HTTP GET " + url);
    String content = null;
    // set http timeout config
    RequestConfig requestConfig = RequestConfig.custom()
            .setConnectionRequestTimeout(conn_timeout)
            .setSocketTimeout(conn_timeout)
            .setConnectTimeout(conn_timeout)
            .build();
    httpget.setConfig(requestConfig);
    try {
      try (CloseableHttpResponse response = closeableHttpClient.execute(httpget)) {
        content = response2String(response);
        if (response.getStatusLine().getStatusCode() == 200) {
          logger.debug("Content : " + content);
        } else {
          logger.info("Failed to get http resource: " + url + ", response: " + content);
        }
      }
    } catch (Exception ex) {
      logger.error(ExceptionUtils.getFullStackTrace(ex));
    } finally {
      httpget.releaseConnection();
    }

    return content;
  }

  public static String httpResource(CloseableHttpClient closeableHttpClient, HttpRequestBase httpRequest, int conn_timeout) {
    String url = httpRequest.getURI().toString();
    String content = null;
    // set http timeout config
    RequestConfig requestConfig = RequestConfig.custom()
            .setConnectionRequestTimeout(conn_timeout)
            .setSocketTimeout(conn_timeout)
            .setConnectTimeout(conn_timeout)
            .build();
    httpRequest.setConfig(requestConfig);
    try {
      try (CloseableHttpResponse response = closeableHttpClient.execute(httpRequest)) {
        content = response2String(response);
        if (response.getStatusLine().getStatusCode() == 200) {
          logger.debug("Content : " + content);
        } else {
          logger.info("Failed to get http resource: " + url + ", response: " + content);
        }
      }
    } catch (Exception ex) {
      logger.error(ExceptionUtils.getFullStackTrace(ex));
    } finally {
      httpRequest.releaseConnection();
    }
    return content;
  }
}
