package com.ebay.dss.zds.state.source.redis;

import com.ebay.dss.zds.state.StateSnapshot;
import com.ebay.dss.zds.state.source.StateSource;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.Serializable;
import java.util.Properties;

import static com.ebay.dss.zds.common.PropertiesUtil.*;

/**
 * Created by tatian on 2020-09-16.
 */
public class RedisStateSource extends StateSource {

  public static final String PREFIX = "zds.state.source.RedisStateSource.";
  public static final String REDIS_ADDRESS = PREFIX + "redisAddress";
  public static final String MAX_TOTAL = PREFIX + "maxTotal";
  public static final String MAX_IDLE = PREFIX + "maxIdle";
  public static final String MIN_IDLE = PREFIX + "minIdle";
  public static final String BLOCK_WHEN_EXHAUSTED = PREFIX + "blockWhenExhausted";
  public static final String MAX_WAIT_MILLIS = PREFIX + "maxWaitMillis";
  public static final String TEST_ON_BORROW = PREFIX + "testOnBorrow";
  public static final String TEST_ON_RETURN = PREFIX + "testOnReturn";
  public static final String TEST_WHILE_IDLE = PREFIX + "testWhileIdle";
  public static final String TIME_BETWEEN_EVICTION_RUNS_MILLIS = PREFIX + "timeBetweenEvictionRunsMillis";
  public static final String MIN_EVICTABLE_IDLE_TIME_MILLIS = PREFIX + "minEvictableIdleTimeMillis";

  private JedisResourcePool resourcePool;

  protected JedisResourcePool getResourcePool() {
    return this.resourcePool;
  }

  public RedisStateSource(Properties properties) {
    super(properties);
    this.resourcePool = buildJedisPool(properties);
  }

  public boolean store(String key, StateSnapshot stateSnapshot) {
    try (Jedis jedis = resourcePool.borrow()) {
      byte[] bytes = RedisObjectSerializer.serialize(stateSnapshot);
      if (bytes != null) {
        jedis.set(key.getBytes(), RedisObjectSerializer.serialize(stateSnapshot));
        return true;
      } else {
        return false;
      }
    }
  }

  public <T extends Serializable> StateSnapshot<T> lookup(String key) {
    try (Jedis jedis = resourcePool.borrow()) {
      byte[] bytes = jedis.get(key.getBytes());
      return bytes != null ? RedisObjectSerializer.deserialize(bytes) : null;
    }
  }

  public void remove(String key) {
    try (Jedis jedis = resourcePool.borrow()) {
      jedis.del(key.getBytes());
    }
  }

  public void reload() {
    // todo: heat up
  }

  public static JedisResourcePool buildJedisPool(Properties properties) {
    String[] address = properties.getProperty(REDIS_ADDRESS, "127.0.0.1:6379").split(",");
    if (address.length == 1) {
      return new JedisStandaloneResourcePool(RedisAddress.fromString(address[0]), properties);
    } else {
      throw new UnsupportedOperationException("No more than one address can be added");
    }
  }

  private static class JedisStandaloneResourcePool implements JedisResourcePool {

    public final String host;
    public final int port;
    private Properties properties;

    private JedisPoolConfig jedisPoolConfig;
    private JedisPool jedisPool;

    public JedisStandaloneResourcePool(RedisAddress address, Properties properties) {
      this.host = address.host;
      this.port = address.port;
      this.properties = properties;
      this.setupJedisPool(properties);
    }

    public JedisStandaloneResourcePool(String host, int port, Properties properties) {
      this.host = host;
      this.port = port;
      this.properties = properties;
      this.setupJedisPool(properties);
    }

    private void setupJedisPool(Properties properties) {
      this.jedisPoolConfig = new JedisPoolConfig();
      jedisPoolConfig.setMaxTotal(getInt(MAX_TOTAL, properties, 10));
      jedisPoolConfig.setMaxIdle(getInt(MAX_IDLE, properties, 10));
      jedisPoolConfig.setMinIdle(getInt(MIN_IDLE, properties, 5));
      jedisPoolConfig.setBlockWhenExhausted(getBoolean(BLOCK_WHEN_EXHAUSTED, properties, true));
      jedisPoolConfig.setMaxWaitMillis(getLong(MAX_WAIT_MILLIS, properties, 15000L));
      jedisPoolConfig.setTestOnBorrow(getBoolean(TEST_ON_BORROW, properties, false));
      jedisPoolConfig.setTestOnReturn(getBoolean(TEST_ON_RETURN, properties, false));
      jedisPoolConfig.setTestWhileIdle(getBoolean(TEST_WHILE_IDLE, properties, true));
      jedisPoolConfig.setTimeBetweenEvictionRunsMillis(getLong(TIME_BETWEEN_EVICTION_RUNS_MILLIS, properties, 60 * 1000L));
      jedisPoolConfig.setMinEvictableIdleTimeMillis(getLong(MIN_EVICTABLE_IDLE_TIME_MILLIS, properties, 60 * 1000L));
      this.jedisPool = new JedisPool(jedisPoolConfig);
    }


    public Jedis borrow() {
      return jedisPool.getResource();
    }

    public void close() {
      this.jedisPool.destroy();
    }

    public String showStatus() {
      return String.format(
              "NumActive: [%s], NumIdle: [%s], NumWaiters: [%s]",
              this.jedisPool.getNumActive(),
              this.jedisPool.getNumIdle(),
              this.jedisPool.getNumWaiters());
    }
  }


  public interface JedisResourcePool {

    Jedis borrow();
    void close();
    default String showStatus() {
      // do nothing by default
      return "No status collected";
    }
  }

  public static class RedisAddress implements Serializable {
    public final String host;
    public final int port;

    public RedisAddress(String host, int port) {
      this.host = host;
      this.port = port;
    }

    public static RedisAddress fromString(String str) {
      String[] host_port = str.split(":");
      String host = host_port[0].trim();
      int port = Integer.valueOf(host_port[1].trim());
      return new RedisAddress(host, port);
    }
  }

}
