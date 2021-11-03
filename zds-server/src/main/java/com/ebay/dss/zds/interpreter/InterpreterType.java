package com.ebay.dss.zds.interpreter;

import com.ebay.dss.zds.interpreter.interpreters.HDFSInterpreter;
import com.ebay.dss.zds.interpreter.interpreters.Interpreter;
import com.ebay.dss.zds.interpreter.interpreters.imitation.ImitationInterpreter;
import com.ebay.dss.zds.interpreter.interpreters.jdbc.JdbcInterpreter;
import com.ebay.dss.zds.interpreter.interpreters.livy.*;

import java.util.Arrays;
import java.util.Collection;

import static com.ebay.dss.zds.interpreter.InterpreterType.EnumType.*;

public class InterpreterType {

  public enum EnumType {

    LIVY_SHARED("livy-shared", ZLivySharedInterpreter.class),
    LIVY_SPARKR("livy-sparkr", ZLivySparkRInterpreter.class),
    LIVY_PYSPARK("livy-pyspark", ZLivyPySparkInterpreter.class),
    JDBC("jdbc", JdbcInterpreter.class),
    SSH_RESTRICTED_HDFS("ssh-restricted-hdfs", HDFSInterpreter.class),
    LIVY_SPARK("livy-spark", ZLivySparkInterpreter.class),
    LIVY_SPARKSQL("livy-sparksql", ZLivySparkSqlInterpreter.class),
    IMITATE("imitate", ImitationInterpreter.class),
    UNKNOWN("unknown", null);

    private String name;
    private Class interpreterClass;

    EnumType(String name, Class interpreterClass) {
      this.name = name;
      this.interpreterClass = interpreterClass;
    }

    public String getName() {
      return this.name;
    }

    public Class getInterpreterClass() {
      return this.interpreterClass;
    }

    public static String getInterpreterName(Class<? extends Interpreter> clasz) {
      for (EnumType type : EnumType.values()) {
          if (type.equals(UNKNOWN)) continue;
          if (type.getInterpreterClass().equals(clasz)) {
              return type.name;
          }
      }
      return null;
    }
  }

  public static EnumType fromString(String type) {
    switch (type) {
      case "livy-spark":
        return LIVY_SPARK;
      case "livy-sparksql":
        return LIVY_SPARKSQL;
      case "ssh-restricted-hdfs":
        return SSH_RESTRICTED_HDFS;
      case "jdbc":
        return JDBC;
      case "livy-pyspark":
        return LIVY_PYSPARK;
      case "livy-sparkr":
        return LIVY_SPARKR;
      case "livy-shared":
        return LIVY_SHARED;
      case "imitate":
        return IMITATE;
      default:
        return UNKNOWN;
    }
  }

  public static Class toClass(String type) {
    return toClass(fromString(type));
  }

  public static Class toClass(EnumType type) {
    return type.interpreterClass;
  }

  public enum ConfType {
    ZDS_LIVY("zds.livy"),
    ZDS_JDBC("zds.jdbc"),
    UNKNOWN("UNKNOWN");
    private String name;

    ConfType(String name) {
      this.name = name;
    }

    public String getName() {
      return this.name;
    }
  }

  private static final Collection<String> livyInterpreterClassNames =
          Arrays.asList(ZLivySparkSqlInterpreter.class.getName(),
                  ZLivyPySparkInterpreter.class.getName(),
                  ZLivySparkRInterpreter.class.getName(),
                  ZLivySharedInterpreter.class.getName());

  private static final Collection<String> jdbcInterpreterClassNames =
          Arrays.asList(JdbcInterpreter.class.getName());

  public static ConfType getConfType(String className) {
    if (livyInterpreterClassNames.contains(className)) {
      return ConfType.ZDS_LIVY;
    } else if (jdbcInterpreterClassNames.contains(className)) {
      return ConfType.ZDS_JDBC;
    } else {
      return ConfType.UNKNOWN;
    }
  }

  public static ConfType getConfType(EnumType interpreterType) {
    String className = interpreterType.getInterpreterClass().getName();
    if (livyInterpreterClassNames.contains(className)) {
      return ConfType.ZDS_LIVY;
    } else if (jdbcInterpreterClassNames.contains(className)) {
      return ConfType.ZDS_JDBC;
    } else {
      return ConfType.UNKNOWN;
    }
  }
}
