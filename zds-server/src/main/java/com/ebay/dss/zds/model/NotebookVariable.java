package com.ebay.dss.zds.model;

import com.ebay.dss.zds.magic.pattern.DynamicReplaceMatcher;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by tatian on 2020-10-27.
 */
public class NotebookVariable {

  public transient static final String PATTERN_STR_A = "\\$\\{(.+?)\\}";
  public transient static final String PATTERN_STR_B = "\\$[a-zA-Z]([\\w\\-]*)(?![\\w\\-\\}])";
  public transient static final Pattern PATTERN_A = Pattern.compile(PATTERN_STR_A);
  public transient static final Pattern PATTERN_B = Pattern.compile(PATTERN_STR_B);

  public volatile String name;
  public volatile String value;
  public final String vargenerator;

  public NotebookVariable(String name, String value, String vargenerator) {
    this.name = name;
    this.value = value;
    this.vargenerator = vargenerator;
  }

  public static String getTrimmedVariableName(String nameWithClause) {
    String varName;
    if (nameWithClause.startsWith("${") && nameWithClause.endsWith("}")) {
      varName = nameWithClause.substring(2, nameWithClause.length() - 1);
    } else if (nameWithClause.startsWith("$") && !nameWithClause.endsWith("}")) {
      varName = nameWithClause.substring(1);
    } else if (nameWithClause.startsWith("'") && nameWithClause.endsWith("'")) {
      varName = nameWithClause.substring(1, nameWithClause.length() - 1);
    } else {
      varName = nameWithClause;
    }
    return varName;
  }

  public static List<String> findAllTrimmedVariables(String text) {
    return findAllVariables(text)
            .stream()
            .map(NotebookVariable::getTrimmedVariableName)
            .collect(Collectors.toList());
  }

  public static List<String> findAllVariables(String text) {
    return DynamicReplaceMatcher
            .matchAll(text, PATTERN_A, PATTERN_B);
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getVargenerator() {
    return vargenerator;
  }
}
