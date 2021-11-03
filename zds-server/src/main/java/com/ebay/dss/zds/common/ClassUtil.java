package com.ebay.dss.zds.common;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.net.URLDecoder;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

/**
 * Created by tatian on 2020-10-10.
 */
public class ClassUtil {

  public static List<Class> getClassFromPackage(String packageName) throws IOException {
    List<Class> clazzs = Lists.newArrayList();
    Enumeration<URL> dirs = Thread.currentThread().getContextClassLoader().getResources(packageName.replace('.',
            '/'));
    while (dirs.hasMoreElements()) {
      URL url = dirs.nextElement();
      if ("file".equalsIgnoreCase(url.getProtocol())) {
        String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
        findClassInPackageByFile(packageName, filePath, true, clazzs);
      }
    }
    return clazzs;
  }

  public static void findClassInPackageByFile(String packageName, String filePath, boolean recursive, List<Class> clazzs) {
    File dir = new File(filePath);
    if (!dir.exists() || !dir.isDirectory()) {
      return;
    }
    File[] dirFiles = dir.listFiles(new FileFilter() {
      @Override
      public boolean accept(File pathname) {
        return (recursive && pathname.isDirectory()) || (pathname.getName().endsWith("class"));
      }
    });
    for (File file : dirFiles) {
      if (file.isDirectory()) {
        findClassInPackageByFile(packageName + "." + file.getName(), file.getAbsolutePath(), recursive, clazzs);
      } else {
        String className = file.getName().substring(0, file.getName().length() - 6);
        try {
          clazzs.add(Thread.currentThread().getContextClassLoader().loadClass(packageName + "." + className));
        } catch (ClassNotFoundException e) {
          e.printStackTrace();
        }
      }
    }
  }

  public static Constructor<?> findConstructorByParams(Class clasz, List<String> paramNames) {
    for (Constructor constructor : clasz.getConstructors()) {
      boolean matched = true;
      Parameter[] parameters = constructor.getParameters();
      if ((parameters == null ? 0 : parameters.length) != paramNames.size()) continue;
      for (Parameter parameter : parameters) {
        if (!paramNames.contains(parameter.getName())) {
          matched = false;
          break;
        }
      }
      if (matched) return constructor;
    }
    return null;
  }

  public static Object[] mapValueByParameterType(Parameter[] parameters, Map<String, String> params) throws Exception {
    if (parameters.length != params.size())
      throw new IllegalArgumentException(String.format("Size not match, parameters: %s, params: %s", parameters.length, params.size()));
    Object[] values = new Object[parameters.length];
    for (int i = 0 ; i < parameters.length; i++) {
      values[i] = stringToObject(params.get(parameters[i].getName()), parameters[i].getType().getName());
    }
    return values;
  }

  public static Object stringToObject(String value, String targetClass) throws Exception {
    Object p;
    if (targetClass.equals(String.class.getName())) {
      p = value;
    } else if (targetClass.equals(Long.class.getName())) {
      p = Long.valueOf(value);
    } else if (targetClass.equals(Integer.class.getName())) {
      p = Integer.valueOf(value);
    } else if (targetClass.equals(Boolean.class.getName())) {
      p = Boolean.valueOf(value);
    } else if (targetClass.equals(Double.class.getName())) {
      p = Double.valueOf(value);
    } else if (targetClass.equals(Float.class.getName())) {
      p = Float.valueOf(value);
    } else if (targetClass.equals("int")) {
      p = Integer.valueOf(value).intValue();
    } else if (targetClass.equals("double")) {
      p = Double.valueOf(value).doubleValue();
    } else if (targetClass.equals("float")) {
      p = Float.valueOf(value).floatValue();
    } else if (targetClass.equals("long")) {
      p = Long.valueOf(value).longValue();
    } else if (targetClass.equals("boolean")) {
      p = Boolean.valueOf(value).booleanValue();
    } else {
      throw new UnsupportedOperationException("Unsupported class transform: " + targetClass);
    }
    return p;
  }

  public static boolean hasAnnotation(Object obj, Class<? extends Annotation> annoClass) {
      return obj.getClass().isAnnotationPresent(annoClass);
  }
}
