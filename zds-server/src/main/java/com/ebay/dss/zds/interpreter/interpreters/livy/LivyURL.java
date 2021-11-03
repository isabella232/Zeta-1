package com.ebay.dss.zds.interpreter.interpreters.livy;

import org.apache.commons.lang.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by tatian on 2019-11-04.
 */
public class LivyURL {

  private final String originalURL;
  private String protocol;
  private String host;
  private LivyLocator locator;

  public LivyURL(String url) {
    assert StringUtils.isNotEmpty(url);
    this.originalURL = url;
    decompose(url);
  }

  private void decompose(String url) {
    Pattern pattern = Pattern.compile("https?://");
    Matcher protocolMatcher = pattern.matcher(url);
    assert protocolMatcher.find(); // the regex is lazy matched
    this.protocol = protocolMatcher.group();
    String host_locator = url.substring(this.protocol.length());
    int portIfHas = host_locator.lastIndexOf(":");
    if (portIfHas > -1) {
      assert portIfHas < host_locator.length() - 1;
      host = host_locator.substring(0, portIfHas);
      locator = new PortLocator(host_locator.substring(portIfHas + 1));
    } else {
      locator = new PatternLocator(host_locator);
      host = ((PatternLocator) locator).getHost();
    }
  }

  public String get() {
    return this.originalURL;
  }

  public LivyLocator getLocator() {
    return this.locator;
  }

  public String withNewLocatorPattern(int pattern) {
    return withNewLocatorPattern(String.valueOf(pattern));
  }

  public String withNewLocatorPattern(String pattern) {
    return this.protocol + this.host + this.locator.with(pattern);
  }

  public interface LivyLocator {
    String getString();
    String with(String newPattern);
  }

  public static class PortLocator implements LivyLocator {

    private String portPattern;

    public PortLocator(String port) {
      this.portPattern = port;
    }

    @Override
    public String getString() {
      return ":" + portPattern;
    }

    @Override
    public String with(String newPattern) {
      return ":" + newPattern;
    }
  }

  public static class PatternLocator implements LivyLocator {

    private final static String LOCATOR_PATTERN_PREFIX = "livy-locator-";

    private String host;
    private String locatorPrefix;
    private String port;
    private String restBody;

    public PatternLocator(String anyStr) {
      int locatorIfHas = anyStr.indexOf(LOCATOR_PATTERN_PREFIX);
      if (locatorIfHas > 1) {
        this.host = anyStr.substring(0, locatorIfHas);
        this.locatorPrefix = LOCATOR_PATTERN_PREFIX;
        int recentSlashIndex = anyStr.indexOf("/", locatorIfHas);
        String fullLocator = anyStr.substring(locatorIfHas,
                recentSlashIndex > locatorIfHas? recentSlashIndex: anyStr.length());
        this.port = fullLocator.substring(this.locatorPrefix.length());
        int restBodyIndex = locatorIfHas + fullLocator.length();
        this.restBody = restBodyIndex >= anyStr.length()? ""
                : anyStr.substring(locatorIfHas + fullLocator.length());
      } else {
        this.host = anyStr;
        this.locatorPrefix = "";
        this.port = "";
        this.restBody = "";
      }
    }

    private String getHost() {
      return this.host;
    }

    public String getLocatorString() {
      return locatorPrefix + port;
    }

    @Override
    public String getString() {
      return locatorPrefix + port + restBody;
    }

    @Override
    public String with(String newPattern) {
      return locatorPrefix + newPattern + restBody;
    }
  }
}
