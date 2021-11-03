package com.ebay.dss.zds.state.source;

import com.ebay.dss.zds.state.StateSnapshot;
import com.ebay.dss.zds.state.annotation.StateSourcePrefer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

import static com.ebay.dss.zds.common.PropertiesUtil.getInt;

/**
 * Created by tatian on 2020-09-18.
 */
public class ComplexStateSource extends StateSource {

  private static final Logger logger = LoggerFactory.getLogger(ComplexStateSource.class);

  public static final String PREFIX = "zds.state.source.ComplexStateSource.";
  public static final String STATE_SOURCE_MAPPING_PREFIX = PREFIX + "stateSourceMapping.";
  public static final String MAX_LOOKUP_DEPTH = PREFIX + "maxLookupDepth";

  private ConcurrentHashMap<String, StateSource> preferToStateSource;
  private ConcurrentHashMap<Class, StateSource> classToStateSource;
  private InMemoryStateSource inMemoryStateSource;
  private CopyOnWriteArrayList<String> skippedClass;
  private int maxDepth;

  private final ReentrantLock stateInitLock = new ReentrantLock();

  public ComplexStateSource(Properties prop) {
    super(prop);
  }

  @Override
  public void reload() {
    this.maxDepth = getInt(MAX_LOOKUP_DEPTH, prop, 20);
    this.preferToStateSource = new ConcurrentHashMap<>();
    this.classToStateSource = new ConcurrentHashMap<>();
    this.inMemoryStateSource = new InMemoryStateSource(prop);
    this.skippedClass = new CopyOnWriteArrayList<>();
    this.skippedClass.add(this.getClass().getName());

    this.preferToStateSource.put(InMemoryStateSource.class.getSimpleName(), this.inMemoryStateSource);
    this.prop.keySet()
            .stream()
            .filter(key -> key.toString().startsWith(STATE_SOURCE_MAPPING_PREFIX))
            .map(Object::toString)
            .forEach(key -> {
              try {
                // this is the short name
                String className = key.substring(STATE_SOURCE_MAPPING_PREFIX.length()).trim();
                String sourceName = prop.getProperty(key).trim();
                StateSource stateSource = StateSourceFactory.createSource(sourceName, prop);
                this.preferToStateSource.put(className, stateSource);
                this.classToStateSource.put(stateSource.getClass(), stateSource);
              } catch (Exception ex) {
                ex.printStackTrace();
              }
            });
  }

  public boolean store(String key, StateSnapshot stateSnapshot) {
    return pickUpStateSource().store(key, stateSnapshot);
  }

  public StateSnapshot lookup(String key) {
    return pickUpStateSource().lookup(key);
  }

  public void remove(String key) {
    pickUpStateSource().remove(key);
  }

  public StateSource pickUpStateSource() {

    StateSource stateSource;
    StackTraceElement[] stes = Thread.currentThread().getStackTrace();

    int depth = 0;
    for (StackTraceElement ste : stes) {
      if (depth == maxDepth) {
        logger.info("Max depth reached: {}, skip", maxDepth);
        break;
      }

      String currentClass = ste.getClassName();
      if (this.skippedClass.contains(currentClass)) {
        logger.info("Skip unchecked class: {}", currentClass);
        continue;
      }

      stateSource = preferToStateSource.get(currentClass);
      if (stateSource != null) {
        logger.info("StateSource: {} picked up by class name", stateSource.getClass().getName());
        return stateSource;
      }
      try {
        Class clasz = Class.forName(currentClass);
        stateSource = preferToStateSource.get(clasz.getSimpleName());
        if (stateSource != null) {
          preferToStateSource.put(currentClass, stateSource);
          logger.info("StateSource: {} picked up by simple classname", stateSource.getClass().getName());
          return stateSource;
        }

        Annotation anno = clasz.getAnnotation(StateSourcePrefer.class);
        if (anno != null) {
          StateSourcePrefer prefer = (StateSourcePrefer) anno;
          Class preferClasz = prefer.prefer();
          if (classToStateSource.containsKey(preferClasz)) {
            stateSource = classToStateSource.get(preferClasz);
            this.preferToStateSource.put(currentClass, stateSource);
            this.preferToStateSource.put(clasz.getSimpleName(), stateSource);
            logger.info("StateSource: {} picked up by annotation", stateSource.getClass().getName());
            return stateSource;
          } else {
            try {
              stateInitLock.lock();
              if (!classToStateSource.containsKey(preferClasz)) {
                stateSource = StateSourceFactory.createSource(preferClasz.getName(), prop);
                classToStateSource.put(preferClasz, stateSource);
                logger.info("StateSource: {} created and picked up by annotation", stateSource.getClass().getName());
              } else {
                stateSource = classToStateSource.get(preferClasz);
                logger.info("StateSource: {} picked up by annotation", stateSource.getClass().getName());
              }
              this.preferToStateSource.put(currentClass, stateSource);
              this.preferToStateSource.put(clasz.getSimpleName(), stateSource);
              return stateSource;
            } finally {
              stateInitLock.unlock();
            }
          }
        } else {
          // if reach here, it means the class either tagged a prefer state source nor configured in the conf
          // so we just skip it
          this.skippedClass.add(currentClass);
        }

      } catch (Exception ex) {
        ex.printStackTrace();
      }
      depth++;
    }
    stateSource = this.inMemoryStateSource;
    logger.info("Can't find any prefer state source, using {}", stateSource.getClass().getName());
    return stateSource;
  }

}
