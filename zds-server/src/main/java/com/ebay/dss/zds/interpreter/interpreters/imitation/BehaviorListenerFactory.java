package com.ebay.dss.zds.interpreter.interpreters.imitation;

import com.ebay.dss.zds.common.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by tatian on 2020-12-02.
 */
public class BehaviorListenerFactory {

  public static final String BEHAVIOR_LISTENER_CLASS = "zds.imitation.behavior.listener.class";

  private static final Logger logger = LoggerFactory.getLogger(BehaviorListenerFactory.class);
  private static List<URL> additionalClassUrls = new CopyOnWriteArrayList<>();

  public static BehaviorListener create(Properties properties) throws Exception {

    final AtomicReference<Exception> exception = new AtomicReference<>();

    BehaviorListener listener = ThreadUtils.withClassLoaderRestore(() -> {
      String className = properties.getProperty(BEHAVIOR_LISTENER_CLASS, ImitationInterpreter.ImitationBehaviorListener.class.getName());
      logger.info("Creating BehaviorListener, class: {}, props: {}", className, properties.toString());
      BehaviorListener behaviorListener = null;
      try {
        try {
          behaviorListener = getClassInstance(className, properties);
          logger.info("BehaviorListener, class: {}", className);
        } catch (ClassNotFoundException cfe) {
          URLClassLoader classLoader = new URLClassLoader((URL[]) additionalClassUrls.toArray());
          Thread.currentThread().setContextClassLoader(classLoader);
          behaviorListener = getClassInstance(className, properties);
          logger.info("BehaviorListener, class: {}", className);
        }
      } catch (Exception ex) {
        exception.set(ex);
      }
      return behaviorListener;
    });

    if (exception.get() != null) throw exception.get();
    return listener;
  }

  private static BehaviorListener getClassInstance(String className, Properties properties) throws Exception {
    return (BehaviorListener) Class.forName(className)
            .getConstructor(Properties.class)
            .newInstance(properties);
  }
}
