package com.ebay.dss.zds.rest.error.match;

import com.ebay.dss.zds.rest.error.ErrorRow;
import com.ebay.dss.zds.model.ExceptionRule;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.*;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.josql.QueryExecutionException;
import org.josql.QueryParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class ExceptionRuleService {
    public final static Logger logger = LoggerFactory.getLogger(ExceptionRuleService.class);

    private final AtomicReference<Set<ExceptionRuleQuery>> ruleQueries = new AtomicReference<>();
    private final ScheduledExecutorService ss = Executors.newScheduledThreadPool(1);
    private final ExceptionRuleRepository db;
    private final RestHighLevelClient client;
    private final String baseErrorIndex;
    private int delay;

    public ExceptionRuleService(ExceptionRuleRepository db,
                                @Value("${zds.exception.rule.refresh.interval:300}") Integer interval,
                                @Value("${zds.exception.rule.regression.index:zds-server-error-*}") String baseErrorIndex,
                                ObjectMapper om,
                                RestHighLevelClient client) {
        this.db = db;
        this.delay = interval;
        this.baseErrorIndex = baseErrorIndex;
        this.client = client;
    }

    private static String formatFirstError(List<ErrorRow> errors, ExceptionRule rule) {
        if (errors == null || errors.size() == 0 || rule.getRegex() == null) {
            return rule.getMessage();
        }

        String message = errors.get(0).getMessage();
        message = message.replaceAll("[\\n|\\r]", "");
        return message.replaceAll(rule.getRegex(), rule.getMessage()).trim();
    }

    public static Set<ExceptionRuleQuery> newEmptyQuerySet() {
        return new TreeSet<>(Comparator.comparingInt(ExceptionRuleQuery::getOrder));
    }

    public int getDelay() {
        return delay;
    }

    public ExceptionRuleService setDelay(int delay) {
        this.delay = delay;
        return this;
    }

    public Set<ExceptionRuleQuery> getRuleQueries() {
        return ruleQueries.get();
    }

    @PostConstruct
    public void init() {
        Set<ExceptionRuleQuery> set = newEmptyQuerySet();
        this.ruleQueries.set(set);
        // init rules
        this.refreshRuleQueries();
        LoadExceptionRuleTask loadTask = new LoadExceptionRuleTask(this);
        ss.scheduleWithFixedDelay(loadTask, delay, delay, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void destroy() {
        ss.shutdown();
    }

    public synchronized void refreshRuleQueries() {
        logger.info("Refreshing exception rules ...");
        Set<ExceptionRuleQuery> newSet = findRuleQueries();
        ruleQueries.set(newSet);
    }

    private Set<ExceptionRuleQuery> findRuleQueries() {
        // set comparator to order elements by order field.
        Set<ExceptionRuleQuery> ruleQueries = new TreeSet<>(Comparator.comparingInt(ExceptionRuleQuery::getOrder));
        //load exception rule from database
        //construct a new set to replace the original one.
        int loaded = 0, failed = 0;
        List<ExceptionRule> rules = db.findAll();
        for (ExceptionRule rule : rules) {
            try {
                ExceptionRuleQuery query = ExceptionRuleQuery.newInstance(rule);
                ruleQueries.add(query);
                logger.trace("Load rule {}, order {}, filter {}", rule.getId(), rule.getOrder(), rule.getFilter());
                loaded++;
            } catch (QueryParseException e) {
                logger.warn("Failed load rule {}, error: {}", rule, e);
                failed++;
            }
        }
        logger.info("Loaded exception rules, size: {}, loaded: {}, failed: {}, id in order {}",
                ruleQueries.size(), loaded, failed, ruleQueries
                        .stream()
                        .map(ExceptionRuleQuery::getRule)
                        .map(rule -> String.format("%d(%d)", rule.getOrder(), rule.getId()))
                        .collect(Collectors.toList()));
        return ruleQueries;
    }

    public RuleMatchResult match(Throwable t) {
        Set<ExceptionRuleQuery> queries = ruleQueries.get();
        if (null == queries) return new RuleMatchResult(false);
        List<ErrorRow> errorRows = ErrorRow.errorRowsFromThrowable(t);

        ErrorLogger.log(errorRows, t);
        return match(errorRows, queries);
    }

    private RuleMatchResult match(List<ErrorRow> errorRows, Set<ExceptionRuleQuery> queries) {
        RuleMatchResult result = new RuleMatchResult(false);
        for (ExceptionRuleQuery query : queries) {
            logger.trace("query rule match: {}, order {}", query.getRule().getId(), query.getRule().getOrder());
            try {
                List<ErrorRow> matchedErrors = query.match(errorRows);
                if (!matchedErrors.isEmpty()) {
                    String formattedMessage = formatFirstError(matchedErrors, query.getRule());
                    result = new RuleMatchResult(true, formattedMessage, query.getRule());
                    break;
                }
            } catch (QueryExecutionException e) {
                logger.error("Match error when processing: " + query.getRule(), e);
            }
        }
        logger.trace("Rule matched: {}, rule id {}", result.isMatched(), result.isMatched() ? result.getMatchedRule().getId() : null);
        return result;
    }

    public RuleMatchResult classifyOne(String index, String id, Set<ExceptionRuleQuery> queries) throws IOException {
        List<ErrorRow> errorRows = new RuleMatchClassifyExampleOne(client, index, id).get();
        return match(errorRows, queries);
    }

    public RuleMatchClassify classify(LocalDate after, LocalDate before, Set<ExceptionRuleQuery> queries) throws IOException {
        RuleMatchClassify classifyResult = new RuleMatchClassify();
        for (RuleMatchClassifyExampleIterator it = new RuleMatchClassifyExampleIterator(client, baseErrorIndex, after, before);
             it.hasNext(); ) {
            RuleMatchClassifyErrorRows rows = it.next();
            if (rows.getErrorRows() == null ||
                    rows.getErrorRows().size() == 0) {
                continue;
            }
            RuleMatchResult result = match(rows.getErrorRows(), queries);
            classifyResult.collectResult(result);
        }
        return classifyResult;
    }

    public RuleMatchClassifyDiff classifyDiff(LocalDate after, LocalDate before,
                                              Set<ExceptionRuleQuery> oldQueries,
                                              Set<ExceptionRuleQuery> newQueries) throws IOException {
        RuleMatchClassifyDiff regression = new RuleMatchClassifyDiff();
        for (RuleMatchClassifyExampleIterator it = new RuleMatchClassifyExampleIterator(client, baseErrorIndex, after, before);
             it.hasNext(); ) {
            RuleMatchClassifyErrorRows rows = it.next();
            if (rows.getErrorRows() == null ||
                    rows.getErrorRows().size() == 0) {
                continue;
            }
            RuleMatchResult oldResult = match(rows.getErrorRows(), oldQueries);
            RuleMatchResult newResult = match(rows.getErrorRows(), newQueries);
            regression.collectResult(oldResult, newResult, rows);
        }
        return regression;
    }

    static class RuleMatchClassifyDiff {
        private final RuleMatchClassify oldRule = new RuleMatchClassify();
        private final RuleMatchClassify newRule = new RuleMatchClassify();
        private final List<RuleMatchedDiff> diffList = new ArrayList<>();

        public void collectResult(RuleMatchResult oldResult, RuleMatchResult newResult, RuleMatchClassifyErrorRows ruleMatchClassifyErrorRows) {
            oldRule.collectResult(oldResult);
            newRule.collectResult(newResult);
            if (notSame(oldResult, newResult)) {
                diffList.add(new RuleMatchedDiff(newResult.isMatched() ?
                        newResult.getMatchedRule().getId() :
                        RuleMatchClassifyUtils.NON_MATCH_ID,
                        oldResult.isMatched() ?
                                oldResult.getMatchedRule().getId() :
                                RuleMatchClassifyUtils.NON_MATCH_ID,
                        ruleMatchClassifyErrorRows));
            }
        }

        public RuleMatchClassify getOldRule() {
            return oldRule;
        }

        public RuleMatchClassify getNewRule() {
            return newRule;
        }

        public List<RuleMatchedDiff> getDiffList() {
            return diffList;
        }

        private boolean notSame(RuleMatchResult oldResult, RuleMatchResult newResult) {
            if (oldResult.isMatched() && newResult.isMatched()) {
                return oldResult.getMatchedRule().getId() != newResult.getMatchedRule().getId();
            } else {
                // xor of old and new result
                return (!oldResult.isMatched() && newResult.isMatched()) ||
                        (oldResult.isMatched() && !newResult.isMatched());
            }
        }
    }

    static class RuleMatchClassify {

        private final Map<String, Long> matchedById = new HashMap<>();
        private long total;
        private long totalMatched;

        public void collectResult(RuleMatchResult result) {
            // not thread safe!!
            if (result == null) {
                return;
            }
            total++;
            if (result.isMatched()) {
                totalMatched++;
                int id = result.getMatchedRule().getId();
                increment(RuleMatchClassifyUtils.getRuleIdString(id));
            }
        }

        private void increment(String id) {
            matchedById.putIfAbsent(id, 0L);
            matchedById.computeIfPresent(id, (integer, aLong) -> aLong + 1);
        }

        public long getTotal() {
            return total;
        }

        public long getTotalMatched() {
            return totalMatched;
        }

        public Map<String, Long> getMatchedById() {
            return matchedById;
        }
    }

    static class RuleMatchedDiff {
        private final String newRuleId;
        private final String oldRuleId;
        private final String docIndex;
        private final String docId;

        public RuleMatchedDiff(int newRuleId, int oldRuleId, RuleMatchClassifyErrorRows changed) {
            this.newRuleId = RuleMatchClassifyUtils.getRuleIdString(newRuleId);
            this.oldRuleId = RuleMatchClassifyUtils.getRuleIdString(oldRuleId);
            this.docIndex = changed.getIndex();
            this.docId = changed.getId();
        }

        public String getNewRuleId() {
            return newRuleId;
        }

        public String getOldRuleId() {
            return oldRuleId;
        }

        public String getDocIndex() {
            return docIndex;
        }

        public String getDocId() {
            return docId;
        }
    }

    static class RuleMatchClassifyUtils {

        static final int TEST_RULE_ID = -1;
        static final int NON_MATCH_ID = -2;

        static String getRuleIdString(int id) {
            switch (id) {
                case TEST_RULE_ID:
                    return "test_rule";
                case NON_MATCH_ID:
                    return "non_match";
                default:
                    return String.valueOf(id);
            }
        }

        static List<ErrorRow> getErrorRows(Map source) {
            if (logger.isTraceEnabled()) {
                logger.trace("Get source {}", source);
            }
            List<ErrorRow> errorRows = new ArrayList<>();
            try {
                List<Map> errorRowMaps = (List<Map>) source.get("errorRows");
                for (Map errorRowMap : errorRowMaps) {
                    ErrorRow errorRow = new ErrorRow();
                    errorRow.setLevel((Integer) errorRowMap.get("level"));
                    errorRow.setMessage((String) errorRowMap.get("message"));
                    errorRow.setClassName((String) errorRowMap.get("className"));
                    errorRow.setFullClassName((String) errorRowMap.get("fullClassName"));
                    errorRow.setClassType(Class.forName((String) errorRowMap.get("classType")));
                    errorRows.add(errorRow);
                }
                if (logger.isTraceEnabled()) {
                    logger.trace("Get error rows {}", errorRows);
                }
            } catch (Exception e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Broken hit to error rows", e);
                }
                errorRows = Collections.emptyList();
            }
            return errorRows;
        }

        static RuleMatchClassifyErrorRows getErrorRows(SearchHit hit) {
            return new RuleMatchClassifyErrorRows(getErrorRows(hit.getSourceAsMap()), hit.getIndex(), hit.getId());
        }
    }

    static class RuleMatchClassifyErrorRows {
        private final List<ErrorRow> errorRows;
        private final String index;
        private final String id;

        public RuleMatchClassifyErrorRows(List<ErrorRow> errorRows, String index, String id) {
            this.errorRows = errorRows;
            this.index = index;
            this.id = id;
        }

        public List<ErrorRow> getErrorRows() {
            return errorRows;
        }

        public String getIndex() {
            return index;
        }

        public String getId() {
            return id;
        }
    }

    static class RuleMatchClassifyExampleOne {
        private final RestHighLevelClient client;
        private final String index;
        private final String id;

        RuleMatchClassifyExampleOne(RestHighLevelClient client, String index, String id) {
            this.client = client;
            this.index = index;
            this.id = id;
        }

        List<ErrorRow> get() throws IOException {
            GetRequest getRequest = new GetRequest(index).id(id);
            GetResponse getResponse = client.get(getRequest, RequestOptions.DEFAULT);
            return RuleMatchClassifyUtils.getErrorRows(getResponse.getSourceAsMap());
        }
    }

    static class RuleMatchClassifyExampleIterator implements Iterator<RuleMatchClassifyErrorRows> {
        private final RestHighLevelClient client;
        private final String baseErrorIndex;
        private final LocalDate after;
        private final LocalDate before;
        private String scrollId;
        private SearchHits hits;
        private int perScrollCursor;

        public RuleMatchClassifyExampleIterator(RestHighLevelClient client,
                                                String baseErrorIndex,
                                                LocalDate after,
                                                LocalDate before) {
            this.client = client;
            this.baseErrorIndex = baseErrorIndex;
            this.after = after;
            this.before = before;
            try {
                init();
            } catch (IOException e) {
                logger.error("Init error", e);
            }
        }

        @Override
        public boolean hasNext() {
            if (nonZeroHits() && reachEndOfHits()) {
                try {
                    refresh();
                } catch (IOException e) {
                    logger.error("Refresh error", e);
                }
            }
            return nonZeroHits();
        }

        @Override
        public RuleMatchClassifyErrorRows next() {
            SearchHit hit = hits.getHits()[perScrollCursor];
            perScrollCursor++;
            return RuleMatchClassifyUtils.getErrorRows(hit);
        }

        private boolean nonZeroHits() {
            return hits != null && hits.getHits().length > 0;
        }

        private boolean reachEndOfHits() {
            return perScrollCursor == hits.getHits().length;
        }

        private void init() throws IOException {
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                    .query(QueryBuilders.rangeQuery("@timestamp")
                            .gte(after.format(DateTimeFormatter.ISO_LOCAL_DATE))
                            .lte(before.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                    .fetchSource(true)
                    .size(50);

            SearchRequest searchRequest = new SearchRequest(baseErrorIndex)
                    .source(searchSourceBuilder)
                    .scroll(TimeValue.MINUS_ONE);

            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            scrollId = searchResponse.getScrollId();
            hits = searchResponse.getHits();
            perScrollCursor = 0;
        }

        private void refresh() throws IOException {
            SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
            scrollRequest.scroll(TimeValue.MINUS_ONE);
            SearchResponse scrollResponse = client.scroll(scrollRequest, RequestOptions.DEFAULT);
            scrollId = scrollResponse.getScrollId();
            hits = scrollResponse.getHits();
            perScrollCursor = 0;
        }

        private void close() throws IOException {
            ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
            clearScrollRequest.addScrollId(scrollId);
            client.clearScroll(clearScrollRequest, RequestOptions.DEFAULT);
        }
    }

}
