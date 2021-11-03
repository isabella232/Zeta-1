package com.ebay.dss.zds.serverconfig;


import org.apache.catalina.connector.Connector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

@Component
public class TomcatContainerConfig implements
  WebServerFactoryCustomizer<TomcatServletWebServerFactory> {
  private final static Logger logger = LoggerFactory.getLogger(TomcatContainerConfig.class);

  @Override
  public void customize(TomcatServletWebServerFactory factory) {
    //factory.setProtocol();
    factory.addConnectorCustomizers(new TomcatConnectorCustomizer() {
      @Override
      public void customize(Connector connector) {
        logger.info("protocol: {}", connector.getProtocol());
      }
    });
  }
}
