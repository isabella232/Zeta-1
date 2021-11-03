package com.ebay.dss.zds.interpreter.interpreters.jdbc;

import com.ebay.dss.zds.interpreter.InterpreterConfiguration;
import com.ebay.dss.zds.kerberos.KerberosContext;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.ebay.dss.zds.interpreter.interpreters.jdbc.Constant.*;

public class JdbcConfUtils {

    private static final Logger logger = LogManager.getLogger();

    public static IJdbcConf of(String nt, String notebookId, Map<String, String> params) {
        JdbcType type = typeOf(params);
        IJdbcConf conf = new JdbcConf();

        conf.setId(genJdbcConfId(nt, notebookId, type));
        conf.setNt(nt);
        conf.setNotebookId(notebookId);
        conf.setJdbcType(type);
        conf.setUser(params.get(USER_KEY));
        conf.setPassword(params.get(PASSWORD_KEY));
        conf.setHost(params.get(HOST_KEY));
        conf.setPort(Integer.parseInt(params.getOrDefault(PORT_KEY, "-1")));
        conf.setDatabase(params.get(DATABASE_KEY));
        conf.setSsl(Boolean.parseBoolean(params.getOrDefault(SSL_KEY, "false")));
        conf.setUrlParams(extractJdbcUrlParams(params));
        conf.setProps(extractProps(params));
        conf.setDataSourceSupplier(defaultDatasourceSupplier(conf));
        conf.setRowMax(extractRowMax(conf));

        return conf;
    }

    public static IJdbcConf unwrap(Properties prop) {
        return (IJdbcConf) prop.get(JDBC_CONF_KEY);
    }

    public static JdbcType typeOf(Map<String, String> params) {
        return JdbcType.valueOf(params.get(JDBC_TYPE_KEY));
    }

    static Map<String, String> extractJdbcUrlParams(Map<String, String> params) {
        return params.keySet().stream()
                .filter(p -> Pattern.matches(JDBC_URL_PARAMS_REGEX, p))
                .collect(Collectors.toMap(
                        p -> p.replaceFirst(JDBC_URL_PARAMS_PREFIX_REGEX, ""),
                        params::get));
    }

    static Map<String, String> extractCarmelParams(Map<String, String> params) {
        return params.keySet().stream()
                .filter(p -> Pattern.matches(JDBC_URL_CARMEL_PARAMS_REGEX, p))
                .collect(Collectors.toMap(
                        p -> p.replaceFirst(JDBC_URL_CARMEL_PARAMS_PREFIX_REGEX, ""),
                        params::get));
    }

    static Properties extractProps(Map<String, String> params) {
        Properties properties = new Properties();
        params.keySet().stream()
                .filter(p -> Pattern.matches(JDBC_PROPS_REGEX, p))
                .forEach(p -> {
                    properties.setProperty(p.replaceFirst(JDBC_PROPS_PREFIX_REGEX, ""), params.get(p));
                });
        return properties;
    }

    static Callable<DataSource> defaultDatasourceSupplier(IJdbcConf conf) {
        return () -> {
            JdbcType jdbcType = conf.getJdbcType();

            HikariDataSource ds = new HikariDataSource();
            ds.setDriverClassName(jdbcType.driverClassName);
            String url = conf.url();
            logger.info("JDBC url: " + url);
            ds.setJdbcUrl(url);
            ds.setMaximumPoolSize(DEFAULT_MAX_POOL_SIZES);
            ds.setUsername(conf.getUser());
            ds.setPassword(conf.getPassword());
            ds.setDataSourceProperties(conf.getProps());
            ds.setMaxLifetime(0);
            ds.setIdleTimeout(TimeUnit.HOURS.toMillis(2));
            ds.setPoolName(conf.getId());
            ds.setConnectionTestQuery(jdbcType.testSQL);
            return ds;
        };
    }

    static String genJdbcConfId(String nt, String notebookId, JdbcType jdbcType) {
        String time = String.valueOf(Instant.now().getEpochSecond());
        return StringUtils.join(new String[]{nt, notebookId, jdbcType.name(), time}, '-');
    }

    static int extractRowMax(IJdbcConf conf) {
        return Integer.parseInt(conf.getProperty(MAX_ROWS_KEY, DEFAULT_MAX_ROWS));
    }

    public static Properties wrap(IJdbcConf conf) {
        Properties wrapper = new Properties();
        wrapper.put(JDBC_CONF_KEY, conf);
        return wrapper;
    }

    @Nullable
    public static IJdbcConf unwrap(InterpreterConfiguration configuration) {
        Object jdbcConf = configuration.getProperties().get(JDBC_CONF_KEY);
        if (jdbcConf instanceof IJdbcConf) {
            return (IJdbcConf) jdbcConf;
        } else {
            return null;
        }
    }

    public static Optional<KerberosContext> findIfHasKerberosContext(IJdbcConf conf) {
        KerberosContext context = null;
        Object maybeKerberosContext = conf.get("KerberosContext");
        if (maybeKerberosContext instanceof KerberosContext) {
          context = (KerberosContext) maybeKerberosContext;
        } else {
          logger.warn("Can't find a kerberos context!");
        }
        return Optional.ofNullable(context);
    }

}
