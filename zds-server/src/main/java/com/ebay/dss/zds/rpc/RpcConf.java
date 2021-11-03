package com.ebay.dss.zds.rpc;

import com.ebay.dss.zds.rpc.serializer.RpcMessageCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.ebay.dss.zds.rpc.RpcConf.ConfVars.*;

/**
 * Created by tatian on 2020-09-22.
 */
public class RpcConf {

  protected static final Logger logger = LoggerFactory.getLogger(RpcConf.class);

  private Map<String, String> properties;

  public RpcConf() {
    this.properties = new HashMap<>();
    ConfVars[] vars = ConfVars.values();
    for (ConfVars v : vars) {
      if (v.getType() == ConfVars.VarType.BOOLEAN) {
        this.properties.put(v.getVarName(), v.getBooleanValue() + "");
      } else if (v.getType() == ConfVars.VarType.LONG) {
        this.properties.put(v.getVarName(), v.getLongValue() + "");
      } else if (v.getType() == ConfVars.VarType.INT) {
        this.properties.put(v.getVarName(), v.getIntValue() + "");
      } else if (v.getType() == ConfVars.VarType.FLOAT) {
        this.properties.put(v.getVarName(), v.getFloatValue() + "");
      } else if (v.getType() == ConfVars.VarType.STRING) {
        this.properties.put(v.getVarName(), v.getStringValue());
      } else {
        throw new RuntimeException("Unsupported VarType");
      }
    }
  }

  public RpcConf(Properties properties) {
    this.properties = new HashMap<>();
    properties.keySet().forEach( key -> this.properties.put(key.toString(), properties.get(key).toString()));
  }

  public RpcConf(Map<String, String> properties) {
    this.properties = properties;
  }

  private String getStringValue(String name, String d) {
    String value = this.properties.get(name);
    if (value != null) {
      return value;
    }
    return d;
  }

  private int getIntValue(String name, int d) {
    String value = this.properties.get(name);
    if (value != null) {
      return Integer.parseInt(value);
    }
    return d;
  }

  private long getLongValue(String name, long d) {
    String value = this.properties.get(name);
    if (value != null) {
      return Long.parseLong(value);
    }
    return d;
  }

  private float getFloatValue(String name, float d) {
    String value = this.properties.get(name);
    if (value != null) {
      return Float.parseFloat(value);
    }
    return d;
  }

  private boolean getBooleanValue(String name, boolean d) {
    String value = this.properties.get(name);
    if (value != null) {
      return Boolean.parseBoolean(value);
    }
    return d;
  }

  public String getString(ConfVars c) {
    return getString(c.name(), c.getVarName(), c.getStringValue());
  }

  public String getString(String envName, String propertyName, String defaultValue) {
    if (System.getenv(envName) != null) {
      return System.getenv(envName);
    }
    if (System.getProperty(propertyName) != null) {
      return System.getProperty(propertyName);
    }

    return getStringValue(propertyName, defaultValue);
  }

  public int getInt(ConfVars c) {
    return getInt(c.name(), c.getVarName(), c.getIntValue());
  }

  public int getInt(String envName, String propertyName, int defaultValue) {
    if (System.getenv(envName) != null) {
      return Integer.parseInt(System.getenv(envName));
    }

    if (System.getProperty(propertyName) != null) {
      return Integer.parseInt(System.getProperty(propertyName));
    }
    return getIntValue(propertyName, defaultValue);
  }

  public long getLong(ConfVars c) {
    return getLong(c.name(), c.getVarName(), c.getLongValue());
  }

  public long getLong(String envName, String propertyName, long defaultValue) {
    if (System.getenv(envName) != null) {
      return Long.parseLong(System.getenv(envName));
    }

    if (System.getProperty(propertyName) != null) {
      return Long.parseLong(System.getProperty(propertyName));
    }
    return getLongValue(propertyName, defaultValue);
  }

  public float getFloat(ConfVars c) {
    return getFloat(c.name(), c.getVarName(), c.getFloatValue());
  }

  public float getFloat(String envName, String propertyName, float defaultValue) {
    if (System.getenv(envName) != null) {
      return Float.parseFloat(System.getenv(envName));
    }
    if (System.getProperty(propertyName) != null) {
      return Float.parseFloat(System.getProperty(propertyName));
    }
    return getFloatValue(propertyName, defaultValue);
  }

  public boolean getBoolean(ConfVars c) {
    return getBoolean(c.name(), c.getVarName(), c.getBooleanValue());
  }

  public boolean getBoolean(String envName, String propertyName, boolean defaultValue) {
    if (System.getenv(envName) != null) {
      return Boolean.parseBoolean(System.getenv(envName));
    }

    if (System.getProperty(propertyName) != null) {
      return Boolean.parseBoolean(System.getProperty(propertyName));
    }
    return getBooleanValue(propertyName, defaultValue);
  }

  public int getRpcServerBossThreadNum() {
    return getInt(ZDS_RPC_SERVER_BOSS_THREAD_NUM);
  }

  public int getRpcServerWorkerThreadNum() {
    return getInt(ZDS_RPC_SERVER_WORKER_THREAD_NUM);
  }

  public int getRpcClientWorkerThreadNum() {
    return getInt(ZDS_RPC_CLIENT_WORKER_THREAD_NUM);
  }

  public int getRpcDispatcherConnectionPoolCore() {
    return getInt(ZDS_RPC_DISPATCHER_THREAD_POOL_CONNECTION_CORE);
  }

  public int getRpcDispatcherConnectionPoolMax() {
    return getInt(ZDS_RPC_DISPATCHER_THREAD_POOL_CONNECTION_MAX);
  }

  public int getRpcDispatcherConnectionPoolKeepAliveSecs() {
    return getInt(ZDS_RPC_DISPATCHER_THREAD_POOL_CONNECTION_KEEP_ALIVE_SECS);
  }

  public int getRpcDispatcherDispatchPoolCore() {
    return getInt(ZDS_RPC_DISPATCHER_THREAD_POOL_DISPATCH_CORE);
  }

  public int getRpcDispatcherDispatchPoolMax() {
    return getInt(ZDS_RPC_DISPATCHER_THREAD_POOL_DISPATCH_MAX);
  }

  public int getRpcDispatcherDispatchPoolKeepAliveSecs() {
    return getInt(ZDS_RPC_DISPATCHER_THREAD_POOL_DISPATCH_KEEP_ALIVE_SECS);
  }

  public int getRpcDispatcherDispatchPoolQueueSize() {
    return getInt(ZDS_RPC_DISPATCHER_THREAD_POOL_DISPATCH_QUEUE_SIZE);
  }

  public int getRpcDispatcherProcessPoolCore() {
    return getInt(ZDS_RPC_DISPATCHER_THREAD_POOL_PROCESS_CORE);
  }

  public int getRpcDispatcherProcessPoolMax() {
    return getInt(ZDS_RPC_DISPATCHER_THREAD_POOL_PROCESS_MAX);
  }

  public int getRpcDispatcherProcessPoolKeepAliveSecs() {
    return getInt(ZDS_RPC_DISPATCHER_THREAD_POOL_PROCESS_KEEP_ALIVE_SECS);
  }

  public int getRpcDispatcherProcessPoolQueueSize() {
    return getInt(ZDS_RPC_DISPATCHER_THREAD_POOL_PROCESS_QUEUE_SIZE);
  }

  public int getRpcStreamingPipeRemoteBufferNum() {
    return getInt(ZDS_RPC_STREAMING_PIPE_REMOTE_BUFFER_NUM);
  }

  public long getRpcStreamingPipeRemoteBufferSize() {
    return getLong(ZDS_RPC_STREAMING_PIPE_REMOTE_BUFFER_SIZE);
  }

  public String getRpcMessageCodec() {
    return getString(ZDS_RPC_CODEC);
  }

  public static enum ConfVars {

    ZDS_RPC_SERVER_BOSS_THREAD_NUM("zds.rpc.server.boss.thread.num", 1),
    ZDS_RPC_SERVER_WORKER_THREAD_NUM("zds.rpc.server.worker.thread.num", 24),
    ZDS_RPC_CLIENT_WORKER_THREAD_NUM("zds.rpc.client.worker.thread.num", 12),
    ZDS_RPC_DISPATCHER_THREAD_POOL_CONNECTION_CORE("zds.rpc.dispatcher.thread.pool.connection.core", 1),
    ZDS_RPC_DISPATCHER_THREAD_POOL_CONNECTION_MAX("zds.rpc.dispatcher.thread.pool.connection.max", 128),
    ZDS_RPC_DISPATCHER_THREAD_POOL_CONNECTION_KEEP_ALIVE_SECS("zds.rpc.dispatcher.thread.pool.connection.keep.alive.secs", 60),
    ZDS_RPC_DISPATCHER_THREAD_POOL_DISPATCH_CORE("zds.rpc.dispatcher.thread.pool.dispatch.core", 10),
    ZDS_RPC_DISPATCHER_THREAD_POOL_DISPATCH_MAX("zds.rpc.dispatcher.thread.pool.dispatch.max", 128),
    ZDS_RPC_DISPATCHER_THREAD_POOL_DISPATCH_KEEP_ALIVE_SECS("zds.rpc.dispatcher.thread.pool.dispatch.keep.alive.secs", 60),
    ZDS_RPC_DISPATCHER_THREAD_POOL_DISPATCH_QUEUE_SIZE("zds.rpc.dispatcher.thread.pool.dispatch.queue.size", 128),
    ZDS_RPC_DISPATCHER_THREAD_POOL_PROCESS_CORE("zds.rpc.dispatcher.thread.pool.process.core", 24),
    ZDS_RPC_DISPATCHER_THREAD_POOL_PROCESS_MAX("zds.rpc.dispatcher.thread.pool.process.max", 128),
    ZDS_RPC_DISPATCHER_THREAD_POOL_PROCESS_KEEP_ALIVE_SECS("zds.rpc.dispatcher.thread.pool.process.keep.alive.secs", 60),
    ZDS_RPC_DISPATCHER_THREAD_POOL_PROCESS_QUEUE_SIZE("zds.rpc.dispatcher.thread.pool.process.queue.size", 128),
    ZDS_RPC_STREAMING_PIPE_REMOTE_BUFFER_NUM("zds.rpc.streaming.pipe.remote.buffer.num", 20),
    ZDS_RPC_STREAMING_PIPE_REMOTE_BUFFER_SIZE("zds.rpc.streaming.pipe.remote.buffer.size", 1024 * 1024 * 100L),

    ZDS_RPC_CODEC("zds.rpc.codec", RpcMessageCodec.class.getName());

    private String varName;
    @SuppressWarnings("rawtypes")
    private Class varClass;
    private String stringValue;
    private VarType type;
    private int intValue;
    private float floatValue;
    private boolean booleanValue;
    private long longValue;


    ConfVars(String varName, String varValue) {
      this.varName = varName;
      this.varClass = String.class;
      this.stringValue = varValue;
      this.intValue = -1;
      this.floatValue = -1;
      this.longValue = -1;
      this.booleanValue = false;
      this.type = VarType.STRING;
    }

    ConfVars(String varName, int intValue) {
      this.varName = varName;
      this.varClass = Integer.class;
      this.stringValue = null;
      this.intValue = intValue;
      this.floatValue = -1;
      this.longValue = -1;
      this.booleanValue = false;
      this.type = VarType.INT;
    }

    ConfVars(String varName, long longValue) {
      this.varName = varName;
      this.varClass = Integer.class;
      this.stringValue = null;
      this.intValue = -1;
      this.floatValue = -1;
      this.longValue = longValue;
      this.booleanValue = false;
      this.type = VarType.LONG;
    }

    ConfVars(String varName, float floatValue) {
      this.varName = varName;
      this.varClass = Float.class;
      this.stringValue = null;
      this.intValue = -1;
      this.longValue = -1;
      this.floatValue = floatValue;
      this.booleanValue = false;
      this.type = VarType.FLOAT;
    }

    ConfVars(String varName, boolean booleanValue) {
      this.varName = varName;
      this.varClass = Boolean.class;
      this.stringValue = null;
      this.intValue = -1;
      this.longValue = -1;
      this.floatValue = -1;
      this.booleanValue = booleanValue;
      this.type = VarType.BOOLEAN;
    }

    public String getVarName() {
      return varName;
    }

    @SuppressWarnings("rawtypes")
    public Class getVarClass() {
      return varClass;
    }

    public int getIntValue() {
      return intValue;
    }

    public long getLongValue() {
      return longValue;
    }

    public float getFloatValue() {
      return floatValue;
    }

    public String getStringValue() {
      return stringValue;
    }

    public boolean getBooleanValue() {
      return booleanValue;
    }

    public VarType getType() {
      return type;
    }

    enum VarType {
      STRING {
        @Override
        void checkType(String value) throws Exception {}
      },
      INT {
        @Override
        void checkType(String value) throws Exception {
          Integer.valueOf(value);
        }
      },
      LONG {
        @Override
        void checkType(String value) throws Exception {
          Long.valueOf(value);
        }
      },
      FLOAT {
        @Override
        void checkType(String value) throws Exception {
          Float.valueOf(value);
        }
      },
      BOOLEAN {
        @Override
        void checkType(String value) throws Exception {
          Boolean.valueOf(value);
        }
      };

      boolean isType(String value) {
        try {
          checkType(value);
        } catch (Exception e) {
          logger.error("Exception in RpcConf while isType", e);
          return false;
        }
        return true;
      }

      String typeString() {
        return name().toUpperCase();
      }

      abstract void checkType(String value) throws Exception;
    }
  }
}
