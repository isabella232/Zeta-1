package com.ebay.dss.zds.state.source;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Created by tatian on 2020-08-26.
 */
public class StateSourceFactory {

  public static final String STATE_SOURCE_CLASS = "zds.state.source.class";

  private static final Logger logger = LoggerFactory.getLogger(StateSourceFactory.class);

  public static StateSource createSource(Properties prop) throws Exception {
    String className = prop.getProperty(STATE_SOURCE_CLASS, InMemoryStateSource.class.getName());
    return createSource(className, prop);
  }

  public static StateSource createSource(String className, Properties prop) throws Exception {

    if (!prop.containsKey(STATE_SOURCE_CLASS)) {
      prop.setProperty(STATE_SOURCE_CLASS, className);
    }
    logger.info("Creating state source, class: {}, props: {}", className, prop.toString());
    StateSource stateSource = (StateSource) Class.forName(className)
            .getConstructor(Properties.class)
            .newInstance(prop);
    logger.info("State source created, class: {}", className);

    return stateSource;
  }
}
