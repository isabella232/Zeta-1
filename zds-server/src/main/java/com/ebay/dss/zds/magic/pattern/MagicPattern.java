package com.ebay.dss.zds.magic.pattern;

import com.ebay.dss.zds.magic.ParserUtils;
import com.ebay.dss.zds.magic.pattern.handle.RefreshVarHandler;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by tatian on 2020-10-23.
 */
public enum MagicPattern implements BasePattern {

  ZETA_MAGIC_REFRESH_VAR("%REFRESH_VAR\\(.+?\\)") {
    @Override
    public String innerContent(String patternStr) {
      return patternStr.substring(patternStr.indexOf("(") + 1, patternStr.lastIndexOf(")")).trim();
    }

    @Override
    public PatternHandler getHandler() {
      return new RefreshVarHandler();
    }
  };

  private Pattern pattern;
  MagicPattern(String pattern) {
    this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
  }

  public Matcher matcher(String text) {
    return this.pattern.matcher(text);
  }
  public abstract String innerContent(String patternStr);
  public abstract PatternHandler getHandler();

  public static class MatcherAndPattern {

    public static final MatcherAndPattern NotMatched = new MatcherAndPattern(false, null, null);
    public final boolean matched;
    public final Matcher matcher;
    public final MagicPattern pattern;
    public MatcherAndPattern(boolean matched, Matcher matcher, MagicPattern pattern) {
      this.matched = matched;
      this.matcher = matcher;
      this.pattern = pattern;
    }

    public boolean isMatched() {
      return this.matched;
    }
  }

  public static MatcherAndPattern tryMatch(String text) {
   return tryMatch(text, null);
  }

  public static MatcherAndPattern tryMatch(String text, List<ParserUtils.Partition> skipArea) {
    for (MagicPattern pattern : MagicPattern.values()) {
      Matcher matcher = pattern.matcher(text);
      if (matcher.find() && !inTouch(matcher.start(), matcher.end() - 1, skipArea)) {
        return new MatcherAndPattern(true, matcher, pattern);
      }
    }
    return MatcherAndPattern.NotMatched;
  }

  public static boolean inTouch(int start, int end, List<ParserUtils.Partition> skipArea) {
    if (skipArea == null) return false;
    for (ParserUtils.Partition partition : skipArea) {
      if (partition.hasOverlap(start, end)) {
        return true;
      }
    }
    return false;
  }
}
