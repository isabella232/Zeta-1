package com.ebay.dss.zds.rest.error.match;

import com.ebay.dss.zds.rest.error.ErrorRow;
import com.ebay.dss.zds.model.ExceptionRule;
import org.josql.Query;
import org.josql.QueryExecutionException;
import org.josql.QueryParseException;
import org.josql.QueryResults;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ExceptionRuleQuery {
  private Query query; //= new Query();
  private ExceptionRule rule;
  private final static String SELECT = "select * from " + ErrorRow.class.getName() + " where ";

  public static void tryFilter(String filter) throws QueryParseException, QueryExecutionException {
    Query query = new Query();
    query.parse(SELECT + filter);
    try{
      throw new RuntimeException("Test exception");
    }catch(Throwable t){
      List<ErrorRow> errors = ErrorRow.errorRowsFromThrowable(t);
      query.execute(errors);
    }
  }

  public static ExceptionRuleQuery newInstance(ExceptionRule rule) throws QueryParseException {
    Query query = new Query();
    query.parse(SELECT + rule.getFilter());
    return new ExceptionRuleQuery(rule, query);
  }

  private ExceptionRuleQuery(ExceptionRule rule, Query query) {
    this.rule = rule;
    this.query = query;
  }

  public List<ErrorRow> match(List<ErrorRow> rows) throws QueryExecutionException {
    synchronized(this) {
      QueryResults results = query.execute(rows);
      List<ErrorRow> errors = results.getResults();
      if ( null == errors ) return new ArrayList<>();
      return errors;
    }
  }

  public ExceptionRule getRule() {
    return rule;
  }

  public int getOrder(){
    return rule.getOrder();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ExceptionRuleQuery that = (ExceptionRuleQuery) o;
    return rule.equals(that.rule);
  }

  @Override
  public int hashCode() {
    return Objects.hash(rule);
  }
}
