package com.ebay.dss.zds.common;

import com.ebay.dss.zds.interpreter.ConfigurationManager;
import com.ebay.dss.zds.interpreter.interpreters.Interpreter;
import com.ebay.dss.zds.interpreter.interpreters.jdbc.IJdbcConf;
import com.ebay.dss.zds.interpreter.interpreters.jdbc.JdbcInterpreter;
import com.ebay.dss.zds.interpreter.interpreters.livy.ZBaseLivyInterpreter;
import org.apache.commons.lang3.StringUtils;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

public enum Metric2Report {


    INTERPRETER_CONNECTION_TIME("ZDS_INTERPRETER_CONNECTION_TIME", "timer"),
    INTERPRETER_STATEMENT_TIME("ZDS_INTERPRETER_STATEMENT_TIME", "timer"),
    NOTEBOOK_PREFERENCE_APPLY_COUNT("ZDS_NOTEBOOK_PREFERENCE_APPLY_COUNT", "counter"),
    ;

    public final String name;
    public final String type;


    Metric2Report(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public static class Utils {

        private static Map<String, String> teradataHostPlatform = new HashMap<>();
        private static Map<String, String> kylinHostPlatform = new HashMap<>();
        private static Map<String, String> hiveHostPlatform = new HashMap<>();

        static {
            teradataHostPlatform.put("hopper.vip.ebay.com", "hopper");
            teradataHostPlatform.put("mozart.lvs.ebay.com", "mozart");
            kylinHostPlatform.put("kylin-qa-p.rno.vip.ebay.com", "kylin-rno-qa");
            kylinHostPlatform.put("kylin-p.rno.vip.ebay.com", "kylin-rno");
            kylinHostPlatform.put("kylin.rno.corp.ebay.com", "kylin-rno-qa");
            kylinHostPlatform.put("kylin-qa.rno.corp.ebay.com", "kylin-rno");
            hiveHostPlatform.put("rnohdc42en0005.rno.ebay.com", "apollo-rno");
            hiveHostPlatform.put("rnohdc42en0006.rno.ebay.com", "apollo-rno");
            hiveHostPlatform.put("hercules-lvs-rm-1.vip.ebay.com", "hercules");
            hiveHostPlatform.put("ares-hv.vip.ebay.com", "ares");
        }

        public static String getTeradataPlatform(String host) {
            return teradataHostPlatform.get(StringUtils.lowerCase(host));
        }

        public static String getLivyPlatform(int clusterId) {
            return ConfigurationManager.Cluster.valueOfClusterId(clusterId);
        }

        public static String getHivePlatform(String host) {
            return hiveHostPlatform.get(StringUtils.lowerCase(host));
        }

        public static String getKylinPlatform(String host) {
            return kylinHostPlatform.get(StringUtils.lowerCase(host));
        }

        public static String getJdbcPlatform(IJdbcConf conf) {
            switch (conf.getJdbcType()) {
                case teradata:
                    return getTeradataPlatform(conf.getHost());
                case hive:
                    return getHivePlatform(conf.getHost());
                case kylin:
                    return getKylinPlatform(conf.getHost());
                default:
                    return null;
            }
        }

        public static void setSomeTagsFromInterpreter(Interpreter interpreter, Map<String, String> tags) {
            if (interpreter != null) {
                tags.put("username", interpreter.getUserName());
                tags.put("interpreter_class", interpreter.getClassName());
                if (interpreter instanceof JdbcInterpreter) {
                    JdbcInterpreter jdbcInterpreter = (JdbcInterpreter) interpreter;
                    IJdbcConf conf = jdbcInterpreter.getConf();
                    tags.put("jdbc_type", conf.getJdbcType().toString());
                    tags.put("platform", getJdbcPlatform(conf));
                } else if (interpreter instanceof ZBaseLivyInterpreter) {
                    ZBaseLivyInterpreter livyInterpreter = (ZBaseLivyInterpreter) interpreter;
                    tags.put("platform", getLivyPlatform(livyInterpreter.getClusterId()));
                }
            }
        }

        public static void setYearMonthTags(Map<String, String> tags) {
            ZonedDateTime zonedDateTime = ZonedDateTime.now(ZoneId.of("UTC"));
            tags.put("year", String.valueOf(zonedDateTime.getYear()));
            tags.put("month", String.valueOf(zonedDateTime.getMonthValue()));
        }
    }
}
