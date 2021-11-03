package com.ebay.dss.zds.magic.pattern;

import com.ebay.dss.zds.magic.ParserUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.ebay.dss.zds.magic.pattern.MagicPattern.inTouch;

/**
 * Created by tatian on 2020-10-26.
 */
public class DynamicReplaceMatcher {

  public final String result;
  public final boolean replaced;

  public DynamicReplaceMatcher(String result, boolean replaced) {
    this.result = result;
    this.replaced = replaced;
  }

  public static DynamicReplaceMatcher iterateAndReplace(Matcher matcher, Function<String, String> mapper) {
    return iterateAndReplace(matcher, mapper, null);
  }

  public static DynamicReplaceMatcher iterateAndReplace(Matcher matcher, Function<String, String> mapper,  List<ParserUtils.Partition> skipArea) {
    StringBuffer sb = new StringBuffer();
    boolean replaced = false;
    while (matcher.find()) {
      if (inTouch(matcher.start(), matcher.end() - 1, skipArea)) continue;
      int cnt = matcher.groupCount();
      for (int i = 0; i < cnt; i++) {
        String mapped = mapper.apply(matcher.group(i));
        matcher.appendReplacement(sb, mapped);
      }
      replaced = true;
    }
    matcher.appendTail(sb);
    return new DynamicReplaceMatcher(sb.toString(), replaced);
  }

  public static DynamicReplaceMatcher matchAllAndReplace(String original, Function<String, String> mapper, Pattern... patterns) {
    return matchAllAndReplace(original, mapper, null, patterns);
  }

  public static DynamicReplaceMatcher matchAllAndReplace(String original, Function<String, String> mapper, List<ParserUtils.Partition> skipArea, Pattern... patterns) {
    DynamicReplaceMatcher dynamicReplaceMatcher = new DynamicReplaceMatcher(original, false);
    boolean replacedOnce = false;
    for (Pattern pattern : patterns) {
      Matcher matcher = pattern.matcher(dynamicReplaceMatcher.result);
      dynamicReplaceMatcher = iterateAndReplace(matcher, mapper, skipArea);
      if (dynamicReplaceMatcher.replaced) replacedOnce = true;
    }
    return new DynamicReplaceMatcher(dynamicReplaceMatcher.result, replacedOnce);
  }

  public static List<String> matchAll(String original, Pattern... patterns) {
    List<String> matched = new ArrayList<>();
    for (Pattern pattern : patterns) {
      Matcher matcher = pattern.matcher(original);
      while (matcher.find()) {
        matched.add(matcher.group());
      }
    }
    return matched;
  }

}
