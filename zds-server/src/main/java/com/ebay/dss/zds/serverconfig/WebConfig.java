package com.ebay.dss.zds.serverconfig;

import com.ebay.dss.zds.common.Constant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Created by wenliu2 on 4/3/18.
 */
@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {
    final Logger logger = LoggerFactory.getLogger(WebConfig.class);

    @Override
    public void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> resolvers) {
        logger.info("configured resolvers: {}", resolvers);
        //resolvers.add(new ExceptionHandlerExceptionResolver());

    }

    @Override
    public void extendHandlerExceptionResolvers(List<HandlerExceptionResolver> resolvers) {
        logger.info("extend resolvers: {}", resolvers);
    }
    // Implement configuration methods...


    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("*")
                .allowedHeaders("*")
                .exposedHeaders(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        Constant.ZDS_SERVER_REQUEST_ID_ATTR);
    }

}
