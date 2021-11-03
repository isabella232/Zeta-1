package com.ebay.dss.zds.state.source;

import com.ebay.dss.zds.state.StateSnapshot;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Created by tatian on 2020-08-25.
 */
public class InMemoryStateSource extends StateSource {

  private static final Logger logger = LoggerFactory.getLogger(InMemoryStateSource.class);

  public static final String PREFIX = "zds.state.source.InMemoryStateSource";

  public static final String CONCURRENCY_LEVEL = PREFIX + ".concurrencyLevel";
  public static final String MAXIMUM_SIZE = PREFIX + ".maximumSize";
  public static final String INITIAL_CAPACITY = PREFIX + ".initialCapacity";
  public static final String SOFT_VALUES = PREFIX + ".softValues";
  public static final String EXPIRE_AFTER_ACCESS = PREFIX + ".expireAfterAccess";
  public static final String EXPIRE_AFTER_WRITE = PREFIX + ".expireAfterWrite";

  private int concurrency_level;
  private int maximum_size;
  private int initial_capacity;
  private boolean soft_values;
  private long expireAfterAccess;
  private long expireAfterWrite;

  private Cache<String, StateSnapshot> states;

  public InMemoryStateSource(Properties prop) {
    super(prop);
  }

  @Override
  public void reload() {
    this.concurrency_level = Integer.valueOf(prop.getProperty(CONCURRENCY_LEVEL, "500"));
    this.maximum_size = Integer.valueOf(prop.getProperty(MAXIMUM_SIZE, "6000"));
    this.initial_capacity = Integer.valueOf(prop.getProperty(INITIAL_CAPACITY, "500"));
    this.soft_values = Boolean.valueOf(prop.getProperty(SOFT_VALUES, "false"));
    this.expireAfterAccess = Long.valueOf(prop.getProperty(EXPIRE_AFTER_ACCESS, "0"));
    this.expireAfterWrite = Long.valueOf(prop.getProperty(EXPIRE_AFTER_WRITE, "0"));

    CacheBuilder builder = CacheBuilder.newBuilder();

    if (concurrency_level > 0) {
      builder.concurrencyLevel(concurrency_level);
    }

    if (initial_capacity > 0) {
      builder.initialCapacity(initial_capacity);
    }

    if (maximum_size > 0) {
      builder.maximumSize(maximum_size);
    }

    if (soft_values) {
      builder.softValues();
    }

    if (expireAfterAccess > 0) {
      builder.expireAfterAccess(expireAfterAccess, TimeUnit.MILLISECONDS);
    } else if (expireAfterWrite > 0) {
      builder.expireAfterWrite(expireAfterWrite, TimeUnit.MILLISECONDS);
    }

    this.states = builder.build();
    logger.info("InMemoryStateSource cache built: \n" +
                    " concurrency_level: {},\n" +
                    " maximum_size: {},\n" +
                    " initial_capacity: {},\n" +
                    " soft_values: {},\n" +
                    " expireAfterAccess: {},\n" +
                    " expireAfterWrite: {}\n",
            concurrency_level,
            initial_capacity,
            maximum_size,
            soft_values,
            expireAfterAccess,
            expireAfterWrite);
  }

  public boolean store(String key, StateSnapshot stateSnapshot) {
    this.states.put(key, stateSnapshot);
    return true;
  }

  public StateSnapshot lookup(String key) {
    return this.states.getIfPresent(key);
  }

  public void remove(String key) {
    this.states.invalidate(key);
  }

}
