package com.ebay.dss.zds.serverconfig;

import com.ebay.dss.zds.common.PropertiesUtil;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.*;
import java.time.Duration;
import java.util.*;

import static org.springframework.http.HttpStatus.Series.CLIENT_ERROR;
import static org.springframework.http.HttpStatus.Series.SERVER_ERROR;

/**
 * Created by zhouhuang on Apr 7, 2018
 */
@Configuration
public class RestClientConfig {

  private static final Logger logger = LogManager.getLogger();

  private RestTemplate getLongTimeoutTemplate(
      RestTemplateBuilder builder,
      HttpComponentsClientHttpRequestFactory requestFactory) {
    return getTemplate(builder, requestFactory,
            Duration.ofMinutes(5), Duration.ofMinutes(10));
  }

  private RestTemplate getOneSecondTimeoutTemplate(
          RestTemplateBuilder builder,
          HttpComponentsClientHttpRequestFactory requestFactory) {
    return getTemplate(builder, requestFactory,
            Duration.ofSeconds(1), Duration.ofSeconds(1));
  }

  private RestTemplate getTemplate(
          RestTemplateBuilder builder,
          HttpComponentsClientHttpRequestFactory requestFactory,
          Duration connectTimeout,
          Duration readTimeout) {
    if (requestFactory != null) {
      builder = builder.requestFactory(() -> requestFactory);
    }
    return builder.setConnectTimeout(connectTimeout)
            .setReadTimeout(readTimeout)
            .build();
  }

  @Bean(name = "kylin-template")
  public RestTemplate kylinTemplate(RestTemplateBuilder builder) {
    return getLongTimeoutTemplate(builder, null);
  }

  @Bean(name = "resttemplate")
  public RestTemplate restTemplate(
      RestTemplateBuilder builder,
      @Qualifier("sslDisabledRequestFactory") HttpComponentsClientHttpRequestFactory requestFactory) {
    return getLongTimeoutTemplate(builder, requestFactory);
  }

  @Bean(name = "doe-template")
  public RestTemplate doeRestTemplate(
          RestTemplateBuilder builder,
          @Qualifier("sslDisabledRequestFactory") HttpComponentsClientHttpRequestFactory requestFactory) {
    return getOneSecondTimeoutTemplate(builder, requestFactory);
  }

  @Bean(name = "alationResttemplate")
  public RestTemplate alationRestTemplate(
      RestTemplateBuilder builder,
      @Qualifier("sslDisabledRequestFactory") HttpComponentsClientHttpRequestFactory requestFactory) {
    RestTemplate restTemplate =  getLongTimeoutTemplate(builder, requestFactory);
    restTemplate.getInterceptors().add(new AlationAPIContextInterceptor());
    return restTemplate;
  }

  @Bean(name = "wsm-template")
  public RestTemplate wsmTemplate(RestTemplateBuilder builder,
                                  @Qualifier("sslDisabledRequestFactory") HttpComponentsClientHttpRequestFactory requestFactory) {
    RestTemplate restTemplate = getLongTimeoutTemplate(builder, requestFactory);
    restTemplate.setInterceptors(Collections.singletonList(new WSMContextInterceptor()));
    restTemplate.setErrorHandler(new WSPResponseErrorHandler());
    return restTemplate;
  }

  @Bean
  ResponseErrorHandler simpleErrorHandler() {
    return new ResponseErrorHandler() {
      @Override
      public boolean hasError(ClientHttpResponse clientHttpResponse) {
        return true;
      }

      @Override
      public void handleError(ClientHttpResponse clientHttpResponse) {

      }
    };
  }

  @Bean(name = "error-handle-rest-template")
  public RestTemplate gitResttemplate(
      RestTemplateBuilder builder,
      @Qualifier("sslDisabledRequestFactory") HttpComponentsClientHttpRequestFactory requestFactory) {
    RestTemplate restTemplate = getLongTimeoutTemplate(builder, requestFactory);
    restTemplate.setErrorHandler(simpleErrorHandler());
    return restTemplate;
  }

  @Bean("ihub-rest-template")
  @Autowired
  public RestTemplate iHubAuthConfiguredRestTemplate(
      @Qualifier("sslDisabledRequestFactory") HttpComponentsClientHttpRequestFactory requestFactory,
      @Value("${auth.ihub.url}") String url) {
    return new RestTemplateBuilder()
        .uriTemplateHandler(new DefaultUriBuilderFactory(url))
        .requestFactory(() -> requestFactory)
        .build();
  }

  @Bean("edge-proxy-rest-template")
  @Autowired
  public RestTemplate edgeProxyRestTemplate(
          RestTemplateBuilder builder,
          @Qualifier("sslDisabledRequestFactory") HttpComponentsClientHttpRequestFactory requestFactory) {
    return getTemplate(builder, requestFactory, Duration.ofSeconds(3), Duration.ofSeconds(30));
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  public HttpComponentsClientHttpRequestFactory sslDisabledRequestFactory()
      throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
    SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom()
        .loadTrustMaterial(TrustAllStrategy.INSTANCE).build();
    SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());
    CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(csf).build();

    HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
    requestFactory.setHttpClient(httpClient);
    return requestFactory;
  }

  class AlationAPIContextInterceptor implements ClientHttpRequestInterceptor {
    @Override
    public ClientHttpResponse intercept(
        HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
        throws IOException {

      HttpHeaders headers = request.getHeaders();
      headers.set("TOKEN","5c45a90d-1c2a-4326-b373-5aed260c30b3");
      return execution.execute(request, body);
    }

  }
  class WSMContextInterceptor implements ClientHttpRequestInterceptor {
    @Override
    public ClientHttpResponse intercept(
        HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
        throws IOException {

      HttpHeaders headers = request.getHeaders();
      headers.setBearerAuth(PropertiesUtil.getDatamoveProperties("wsm.api.token"));
      return execution.execute(request, body);
    }

  }

  class WSPResponseErrorHandler implements ResponseErrorHandler {

    @Override
    public boolean hasError(ClientHttpResponse httpResponse)
        throws IOException {

      return (
          httpResponse.getStatusCode().series() == CLIENT_ERROR
              || httpResponse.getStatusCode().series() == SERVER_ERROR);
    }

    @Override
    public void handleError(ClientHttpResponse httpResponse)
        throws IOException {

      if (httpResponse.getStatusCode()
          .series() == HttpStatus.Series.SERVER_ERROR) {
        // handle SERVER_ERROR
      } else if (httpResponse.getStatusCode()
          .series() == HttpStatus.Series.CLIENT_ERROR) {
        // handle CLIENT_ERROR
      }
    }
  }
}