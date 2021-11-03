package com.ebay.dss.zds.service.ace;

import com.ebay.dss.zds.model.ace.*;
import com.ebay.dss.zds.serverconfig.AceSearchFaqProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.search.MultiSearchRequest;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AceSearchFaqService implements AceSearchService {

    private static final Logger logger = LogManager.getLogger();
    private static final String TYPE = "faq";
    private final RestHighLevelClient client;
    private final SearchRequestStrategy searchRequestStrategy;
    private final HitParser<AceQuestionES> aceQuestionESHitParser = new AceQuestionParser();
    private final Function<Object, String> resultClassifier;

    @Autowired
    public AceSearchFaqService(RestHighLevelClient client,
                               AceSearchFaqProperties properties) {
        this.client = client;
        this.searchRequestStrategy = new SimpleStrategy(properties.getIndices(), properties.getFields(), true);
        logger.info("Faq search use indices: {}", properties.getIndices());
        logger.info("Faq search use fields: {}", properties.getFields());
        this.resultClassifier = o -> {
            if (o instanceof AceQuestionES) {
                return "faq";
            } else {
                return "unknown";
            }
        };
    }

    @Override
    public Optional<AceSearchEntry> search(AceSearchOptions options) {
        try {
            return Optional.of(simpleHintSearchInternal(options));
        } catch (Exception e) {
            logger.error("elasticsearch error ", e);
            return Optional.empty();
        }
    }

    @Override
    public Collection<String> scopes() {
        return Collections.singleton(TYPE);
    }

    private AceSearchEntry simpleHintSearchInternal(AceSearchOptions options) throws IOException {
        List<SearchRequest> requests = searchRequests(options);
        MultiSearchRequest mRequest = new MultiSearchRequest();
        for (SearchRequest request : requests) {
            mRequest.add(request);
        }

        MultiSearchResponse resp = esSearch(mRequest);

        AceSearchEntry res = new AceSearchEntry();
        for (MultiSearchResponse.Item item : resp.getResponses()) {
            if (item.isFailure()) {
                logger.error(item.getFailureMessage());
                continue;
            }
            res.putAll(parseSearch(item));
        }
        return res;
    }

    MultiSearchResponse esSearch(MultiSearchRequest mRequest) throws IOException {
        return client.msearch(mRequest, RequestOptions.DEFAULT);
    }

    SearchHit[] getHits(MultiSearchResponse.Item item) {
        return item.getResponse().getHits().getHits();
    }

    private AceSearchEntry parseSearch(MultiSearchResponse.Item item) {
        AceSearchEntry res = new AceSearchEntry();
        SearchHit[] hits = getHits(item);
        Map<String, List<Object>> m = Arrays.stream(hits)
                .map(aceQuestionESHitParser::parse)
                .collect(Collectors.groupingBy(resultClassifier));
        res.putAll(m);
        return res;
    }

    private List<SearchRequest> searchRequests(AceSearchOptions options) {
        return Collections.singletonList(searchRequestStrategy.build(options));
    }

    interface SearchRequestStrategy {
        SearchRequest build(AceSearchOptions query);
    }

    interface HitParser<T> {
        T parse(SearchHit hit);
    }

    static class SimpleStrategy implements SearchRequestStrategy {
        private final List<String> indices;
        private final List<String> fields;
        private final boolean escape;

        private SimpleStrategy(List<String> indices, List<String> fields, boolean escape) {
            this.fields = fields;
            this.indices = indices;
            this.escape = escape;
        }

        public boolean isEscape() {
            return escape;
        }

        public List<String> getIndices() {
            return indices;
        }

        public List<String> getFields() {
            return fields;
        }

        @Override
        public SearchRequest build(AceSearchOptions options) {
            QueryStringQueryBuilder queryBuilder = QueryBuilders.queryStringQuery(options.getQuery())
                    .escape(escape);
            for (String field : fields) {
                String[] fieldComponents = field.split("\\^", -1);
                if (fieldComponents.length == 1) {
                    queryBuilder.field(fieldComponents[0]);
                } else if (fieldComponents.length == 2) {
                    queryBuilder.field(fieldComponents[0], Float.parseFloat(fieldComponents[1]));
                }
            }
            return new SearchRequest()
                    .indices(indices.toArray(new String[0]))
                    .source(new SearchSourceBuilder()
                            .query(queryBuilder)
                            .size(options.getSize()));
        }
    }

    static class AceQuestionParser implements HitParser<AceQuestionES> {

        @Override
        public AceQuestionES parse(SearchHit hit) {
            Map<String, Object> source = hit.getSourceAsMap();
            return new AceQuestionES()
                    .setQuestionId(Integer.valueOf(hit.getId()))
                    .setTitle((String) source.get("title"))
                    .setSubmitter((String) source.get("submitter"))
                    .setCreateTime(ZonedDateTime.parse((String) source.get("create_time")))
                    .setUpdateTime(ZonedDateTime.parse((String) source.get("update_time")));
        }
    }

}
