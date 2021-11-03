package com.ebay.dss.zds.rest.error.match;

import com.ebay.dss.zds.model.ExceptionRule;

public class RuleMatchResult {
  private boolean matched = false;
  private String formattedMessage;
  private ExceptionRule matchedRule;

  public RuleMatchResult(boolean matched) {
    this.matched = matched;
  }

  public RuleMatchResult(boolean matched, String formattedMessage, ExceptionRule matchedRule) {
    setFormattedMessage(formattedMessage);
    setMatched(matched);
    setMatchedRule(matchedRule);
  }

  public boolean isMatched() {
    return matched;
  }

  public void setMatched(boolean matched) {
    this.matched = matched;
  }

  public String getFormattedMessage() {
    return formattedMessage;
  }

  public void setFormattedMessage(String formattedMessage) {
    this.formattedMessage = formattedMessage;
  }

  public ExceptionRule getMatchedRule() {
    return matchedRule;
  }

  public void setMatchedRule(ExceptionRule matchedRule) {
    this.matchedRule = matchedRule;
  }
}
