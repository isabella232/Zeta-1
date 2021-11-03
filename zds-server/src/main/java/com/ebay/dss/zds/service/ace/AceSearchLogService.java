package com.ebay.dss.zds.service.ace;

import com.ebay.dss.zds.model.ace.*;
import com.ebay.dss.zds.serverconfig.ElasticConfig;
import com.ebay.dss.zds.service.ElasticLogService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.index.IndexRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

@Service
public class AceSearchLogService {

    private static final Logger logger = LogManager.getLogger();
    private final ElasticLogService service;
    private final ElasticConfig.ElasticLogAceProperties properties;

    @Autowired
    public AceSearchLogService(@Nullable ElasticLogService service,
                               ElasticConfig.ElasticLogAceProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    public void log(AceSearchOptions options, AceSearchEntry entry) {
        if (Objects.nonNull(service) && properties.getEnabled()) {
            service.add(new AceSearchLog(options, entry));
        }
    }

    private class AceSearchLog extends AbstractMap.SimpleImmutableEntry<AceSearchOptions, AceSearchEntry>
            implements ElasticLogService.Indexable {

        AceSearchLog(AceSearchOptions key, AceSearchEntry value) {
            super(key, value);
        }

        @Override
        public Optional<IndexRequest> toIndexRequest() {
            if (this.getKey() == null) {
                return Optional.empty();
            }

            ZonedDateTime dateTime = ZonedDateTime.now(ZoneOffset.UTC);
            String index = properties.getIndexPrefix() + "-" + dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE);
            Map<String, Object> sourceMap = new HashMap<>();
            AceSearchOptions simpleOptions = this.getKey();
            // setup basic fields
            sourceMap.put("time", dateTime.toString());
            sourceMap.put("query", simpleOptions.getQuery());
            sourceMap.put("scopes", simpleOptions.getScopes());

            // check search result in map, save count and id/name of results
            if (this.getValue() != null) {
                AceSearchEntry simpleEntry = this.getValue();
                simpleEntry.keySet()
                        .forEach(k -> {
                            long cnt = count(simpleEntry.get(k));
                            if (cnt > 0) {
                                sourceMap.put(k + "_cnt", cnt);
                                sourceMap.put(k + "_ids", collectIdentities(simpleEntry.get(k)));
                            }
                        });
                logger.info("search result of {}, {}", this.getKey(), sourceMap);
            }
            return Optional.of(new IndexRequest(index).source(sourceMap));
        }


        private long count(Collection<?> set) {
            return Objects.nonNull(set) ? set.size() : 0;
        }

        private Object[] collectIdentities(Collection<?> set) {
            return Objects.nonNull(set) ? set.stream().map(entry -> {
                if (entry instanceof AceQuestionES) {
                    return ((AceQuestionES) entry).getQuestionId();
                } else if (entry instanceof Map) {
                    Map tmp = (Map) entry;
                    if (tmp.containsKey("id")) {
                        return tmp.get("id");
                    } else if (tmp.containsKey("name")) {
                        return tmp.get("name");
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            })
                    .filter(Objects::nonNull)
                    .toArray()
                    : new String[0];
        }
    }

}
