package com.ebay.dss.zds.interpreter.interpreters.imitation.behavior;

import com.ebay.dss.zds.common.ClassUtil;
import com.ebay.dss.zds.interpreter.interpreters.imitation.Behavior;
import com.ebay.dss.zds.interpreter.interpreters.imitation.ImitationInterpreter;
import org.codehaus.plexus.util.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by tatian on 2020-12-02.
 */
public class BehaviorSimulator {

  public static Logger logger = LoggerFactory.getLogger(BehaviorSimulator.class);

  private static class LazyCachedBehaviors {

    public static final List<Class> behaviors = Arrays.asList(
            Do.class, Generate.class, Loop.class, Sleep.class);

    @Deprecated
    private static List<Class> tryFindAllBehaviors() {
      try {
        return ClassUtil
                .getClassFromPackage(BehaviorSimulator.class.getPackage().getName())
                .stream().filter(behavior -> Arrays.asList(behavior.getInterfaces()).contains(Behavior.class))
                .collect(Collectors.toList());
      } catch (Exception ex) {
        logger.error(ExceptionUtils.getFullStackTrace(ex));
        return null;
      }
    }
  }

  public static Behavior simulate(String someSimulationText) throws Exception {
    Pattern pattern = Pattern.compile(".*?\\((.*?=.*?,?)*\\)");
    Matcher matcher = pattern.matcher(someSimulationText);
    if (matcher.find()) {
      String simulationPattern = matcher.group().trim();
      int firstClause = simulationPattern.indexOf("(");
      int lastClause = simulationPattern.length() - 1;
      String behaviorClass = simulationPattern.substring(0, firstClause).trim();
      Class someClasz = lookupBehaviorClass(behaviorClass);
      if (someClasz != null) {
        String paramsBody = simulationPattern.substring(firstClause + 1, lastClause).trim();
        if (paramsBody.length() > 0) {
          Map<String, String> params = new HashMap<>();
          String[] kvs = paramsBody.split(",");
          for (String kv : kvs) {
            String[] k_v = kv.trim().split("=");
            if (k_v.length != 2) throw new RuntimeException("Not a valid params format: " + paramsBody);
            String key = k_v[0].trim();
            String value = k_v[1].trim();
            params.put(key, value);
          }
          Constructor constructor = ClassUtil.findConstructorByParams(someClasz, new ArrayList<>(params.keySet()));
          if (constructor == null) throw new RuntimeException("Cant find any available constructor for: " + simulationPattern);
          Object[] constructorValues = ClassUtil.mapValueByParameterType(constructor.getParameters(), params);
          return (Behavior) constructor.newInstance(constructorValues);
        } else {
          return (Behavior) someClasz.getConstructor().newInstance();
        }
      } else {
        throw new ClassCastException(behaviorClass);
      }
    } else {
      throw new RuntimeException("Unknown pattern: " + someSimulationText);
    }
  }

  private static Class lookupBehaviorClass(String name) {
    List<Class> behaviors = LazyCachedBehaviors.behaviors;
    if (behaviors != null && behaviors.size() > 0) {
      for (Class clasz : behaviors) {
        if (clasz.getSimpleName().equals(name) || clasz.getName().equals(name) || clasz.getCanonicalName().equals(name)) return clasz;
      }
    }
    return null;
  }
}
