package com.ebay.dss.zds.magic.pattern;

import java.util.regex.Matcher;

/**
 * Created by tatian on 2021/4/15.
 */
public interface BasePattern {

  Matcher matcher(String text);
}
