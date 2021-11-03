package com.ebay.dss.zds.service.ace;

import com.ebay.dss.zds.model.ace.AceSearchEntry;
import com.ebay.dss.zds.model.ace.AceSearchOptions;
import com.ebay.dss.zds.model.ace.DoeObjectScopes;
import com.ebay.dss.zds.model.ace.DoeSearchResult;
import com.ebay.dss.zds.serverconfig.AceSearchMetadataProxyProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.CaseFormat;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class AceSearchMetadataProxyV2Service implements AceSearchService {

    private static final Logger LOGGER = LogManager.getLogger();
    private final RestTemplate rest;
    private final AceSearchMetadataProxyProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Collection<String> scopes = new HashSet<>();

    public AceSearchMetadataProxyV2Service(@Qualifier("doe-template") RestTemplate rest,
                                           AceSearchMetadataProxyProperties properties) {
        this.rest = rest;
        this.properties = properties;
    }

    private static Map<String, Object> extractSource(Object o) {
        Map<String, Object> formattedSource = new HashMap<>();
        Map<?, ?> source = (Map<?, ?>) ((Map<?, ?>) o).get("_source");
        for (Object k : source.keySet()) {
            if (k instanceof String) {
                String formattedKey = ((String) k).trim().replaceAll(" ", "_");
                formattedKey = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, formattedKey);
                formattedSource.put(formattedKey, source.get(k));
            }
        }
        return formattedSource;
    }

    @Override
    public Optional<AceSearchEntry> search(AceSearchOptions options) {
        try {
            Collection<String> validScopes = validScopes(options);
            String scopeStr = StringUtils.join(validScopes, ",");
            Map<String, Object> params = new HashMap<>();
            params.put("keywords", options.getQuery());
            params.put("objs", scopeStr);
            params.put("size", properties.getSearchLimit());
            ResponseEntity<DoeSearchResult> resp = rest.getForEntity(
                    properties.getSearchUri(),
                    DoeSearchResult.class,
                    params);
            if (!resp.getStatusCode().is2xxSuccessful()) {
                return Optional.empty();
            }
            return Optional.of(toEntry(resp.getBody(), validScopes));
        } catch (Exception e) {
            LOGGER.error("metadata service error ", e);
            return Optional.empty();
        }
    }

    private Collection<String> validScopes(AceSearchOptions options) {
        Set<String> validScopes = new HashSet<>();
        for (String scope : options.getScopes()) {
            if (scopes().contains(scope)) {
                validScopes.add(scope);
            }
        }
        return validScopes;
    }

    @Override
    public Collection<String> scopes() {
        return getOrInitializeFromSupplier(this::getDoeObjectTypes);
    }

    private Collection<String> getDoeObjectTypes() {
        DoeObjectScopes doeObjectScopes = null;
        try {
            doeObjectScopes = rest.getForObject(properties.getTypeUri(), DoeObjectScopes.class);
        } catch (Exception e) {
            LOGGER.error("Query scopes of metadat from doe failed", e);
        }
        if (doeObjectScopes == null) {
            LOGGER.warn("Get null result from doe");
            return Collections.emptySet();
        }
        return doeObjectScopes.getValue();
    }

    private Collection<String> getOrInitializeFromSupplier(Supplier<Collection<String>> typeSup) {
        if (scopes.isEmpty()) {
            synchronized (this) {
                if (scopes.isEmpty()) {
                    Collection<String> newTypes = typeSup.get();
                    LOGGER.info("New scopes fetched {}", newTypes);
                    scopes.addAll(newTypes);
                }
            }
        }
        return scopes;
    }

    private AceSearchEntry toEntry(DoeSearchResult result, Collection<String> scopes) {
        AceSearchEntry entry = new AceSearchEntry();
        if (result == null ||
                result.getValue() == null ||
                result.getValue().getResult() == null) {
            return entry;
        }
        scopes.forEach(obj -> {
            assignEntries(obj, result, entry);
        });
        return entry;
    }

    private void assignEntries(String type, DoeSearchResult result, AceSearchEntry entry) {
        List<Object> entries = result.getValue()
                .getResult()
                .stream()
                .filter(v -> type.equals(v.getType()))
                .map(this::toEntry)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        entry.put(type, entries);

    }

    private Object toEntry(DoeSearchResult.Result result) {
        try {
            return getSource(result);
        } catch (Exception e) {
            LOGGER.warn(result.getType() + " search proxy error on parsing content", e);
            return null;
        }
    }

    private Map<String, Object> getSource(DoeSearchResult.Result result) {
        try {
            Map<?, ?> map = objectMapper.readValue(result.getContent(), Map.class);
            return extractSource(map);
        } catch (IOException e) {
            LOGGER.warn("Cannot get metadata content source", e);
            return Collections.emptyMap();
        }
    }

}
