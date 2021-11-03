package com.ebay.dss.zds.interpreter.interpreters.jdbc;

import com.ebay.dss.zds.kerberos.KerberosContext;
import org.apache.hive.jdbc.HiveConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.datasource.ConnectionProxy;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.lang.Nullable;

import javax.sql.DataSource;
import java.io.Closeable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;

public class KerberosDataSource implements DataSource, Closeable {

  private static final Logger logger = LogManager.getLogger();
  private volatile KerberosContext context;
  private volatile Connection connection;
  private volatile Connection proxyConnection;
  private IJdbcConf iJdbcConf;

  private final Object connectionMonitor = new Object();

  public KerberosDataSource(KerberosContext context, IJdbcConf iJdbcConf) {
    this.context = context;
    this.iJdbcConf = iJdbcConf;
  }

  public KerberosDataSource(KerberosContext context) {
    this(context, null);
  }

  // this method will be called every time when a sql is executed by the jdbc template
  @Override
  public Connection getConnection() throws SQLException {
    synchronized (connectionMonitor) {
      try {
        if (proxyConnection != null) return proxyConnection;
        return context.doAs(() -> {
          Class.forName(iJdbcConf.getJdbcType().driverClassName);
          int targetTimeout = getTargetLoginTimeout();
          if (targetTimeout >=0) {
            logger.info("Current Login timeout before set: " + DriverManager.getLoginTimeout());
            DriverManager.setLoginTimeout(targetTimeout);
            logger.info("Current Login timeout after set: " + DriverManager.getLoginTimeout());
          } else {
            logger.info("The target time out is set to a negative number, follow current value:  "
                    + DriverManager.getLoginTimeout());
          }
          this.connection = DriverManager.getConnection(iJdbcConf.url());
          this.proxyConnection = getCloseSuppressingConnectionProxy(this.connection);
          return this.proxyConnection;
        });
      } catch (Exception e) {
        throw new SQLException(e);
      }
    }
  }

  public int getTargetLoginTimeout() {
    return Integer.valueOf(iJdbcConf.getProperty("connect.timeout", "900000"));
  }


  public Connection getTargetConnection() {
    return this.connection;
  }

  public KerberosContext getKerberosContext() {
    return this.context;
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    return this.getConnection();
  }

  public static KerberosDataSource newInstance(IJdbcConf conf, KerberosContext context) {
    return new KerberosDataSource(context, conf);
  }

  @Override
  public java.io.PrintWriter getLogWriter() throws SQLException {
    throw new SQLException("Method not supported");
  }

  @Override
  public void setLogWriter(java.io.PrintWriter out) throws SQLException {
    throw new SQLException("Method not supported");
  }

  @Override
  public void setLoginTimeout(int seconds) throws SQLException {
    throw new SQLException("Method not supported");
  }

  @Override
  public int getLoginTimeout() throws SQLException {
    return DriverManager.getLoginTimeout();
  }

  @Override
  public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
    return java.util.logging.Logger.getLogger("global");
  }

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    if (iface.isInstance(this)) {
      return (T) this;
    } else {
      throw new SQLException("DataSource of type [" + this.getClass().getName() + "] cannot be unwrapped as [" + iface.getName() + "]");
    }
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return iface.isInstance(this);
  }

  @Override
  public void close() {
    synchronized (connectionMonitor) {
      try {
        if (connection != null) {
          connection.close();
          connection = null;
          proxyConnection = null;
        }
      } catch (Exception ex) {
        logger.error(ex.toString());
      }
    }
  }

  public String getUrl() {
    return this.iJdbcConf.url();
  }

  public String getUsername() {
    return "";
  }

  public String getPassword() {
    return "";
  }


  protected Connection getCloseSuppressingConnectionProxy(Connection target) {
    return (Connection) Proxy.newProxyInstance(ConnectionProxy.class.getClassLoader(), new Class[]{ConnectionProxy.class}, new CloseSuppressingInvocationHandler(target));
  }

  private static class CloseSuppressingInvocationHandler implements InvocationHandler {
    private final Connection target;

    public CloseSuppressingInvocationHandler(Connection target) {
      this.target = target;
    }

    @Nullable
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if (method.getName().equals("equals")) {
        return proxy == args[0];
      } else if (method.getName().equals("hashCode")) {
        return System.identityHashCode(proxy);
      } else {
        if (method.getName().equals("unwrap")) {
          if (((Class)args[0]).isInstance(proxy)) {
            return proxy;
          }
        } else if (method.getName().equals("isWrapperFor")) {
          if (((Class)args[0]).isInstance(proxy)) {
            return true;
          }
        } else {
          if (method.getName().equals("close")) {
            return null;
          }

          if (method.getName().equals("isClosed")) {
            return this.target.isClosed();
          }

          if (method.getName().equals("getTargetConnection")) {
            return this.target;
          }
        }

        try {
          return method.invoke(this.target, args);
        } catch (InvocationTargetException ite) {
          throw ite.getTargetException();
        }
      }
    }
  }


}
