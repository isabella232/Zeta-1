package com.ebay.dss.zds.rest.error.match;

import com.ebay.dss.zds.model.ExceptionRule;
import org.josql.QueryParseException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Set;

@RestController
public class ExceptionRuleClassifyController {

    private final ExceptionRuleService service;

    public ExceptionRuleClassifyController(ExceptionRuleService service) {
        this.service = service;
    }

    @PostMapping("/ex_rule/classify")
    public ExceptionRuleService.RuleMatchClassify classify(
            @RequestParam(
                    required = false,
                    defaultValue = "#{T(java.time.LocalDate).now().minusMonths(1)}")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate after,
            @RequestParam(
                    required = false,
                    defaultValue = "#{T(java.time.LocalDate).now().minusDays(1)}")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate before,
            @RequestParam(required = false, defaultValue = "false") Boolean withCurrent,
            @RequestBody ExceptionRule rule) throws QueryParseException, IOException {
        rule.setId(ExceptionRuleService.RuleMatchClassifyUtils.TEST_RULE_ID);
        Set<ExceptionRuleQuery> queries = ExceptionRuleService.newEmptyQuerySet();
        if (withCurrent) {
            queries.addAll(service.getRuleQueries());
        }
        queries.add(ExceptionRuleQuery.newInstance(rule));
        return service.classify(after, before, queries);
    }


    @PostMapping("/ex_rule/classify-diff")
    public ExceptionRuleService.RuleMatchClassifyDiff classify(
            @RequestParam(
                    required = false,
                    defaultValue = "#{T(java.time.LocalDate).now().minusMonths(1)}")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate after,
            @RequestParam(
                    required = false,
                    defaultValue = "#{T(java.time.LocalDate).now().minusDays(1)}")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate before,
            @RequestBody ExceptionRule rule) throws QueryParseException, IOException {
        rule.setId(ExceptionRuleService.RuleMatchClassifyUtils.TEST_RULE_ID);
        Set<ExceptionRuleQuery> oldQueries = ExceptionRuleService.newEmptyQuerySet();
        Set<ExceptionRuleQuery> newQueries = ExceptionRuleService.newEmptyQuerySet();
        Set<ExceptionRuleQuery> current = service.getRuleQueries();
        oldQueries.addAll(current);
        newQueries.addAll(current);
        newQueries.add(ExceptionRuleQuery.newInstance(rule));
        return service.classifyDiff(after, before, oldQueries, newQueries);
    }

    @PostMapping("/ex_rule/testing")
    public RuleMatchResult classify(String index, String id) throws IOException {
        Set<ExceptionRuleQuery> current = service.getRuleQueries();
        return service.classifyOne(index, id, current);
    }
}
