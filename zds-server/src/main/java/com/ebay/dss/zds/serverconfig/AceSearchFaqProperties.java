package com.ebay.dss.zds.serverconfig;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
@ConfigurationProperties("zds.ace.search.service.faq")
public class AceSearchFaqProperties {

    private List<String> indices = Collections.singletonList("faq-question-prod");
    private List<String> fields = Arrays.asList("title", "content^0.5", "title.ngram^0.5");

    public List<String> getIndices() {
        return indices;
    }

    public AceSearchFaqProperties setIndices(List<String> indices) {
        this.indices = indices;
        return this;
    }

    public List<String> getFields() {
        return fields;
    }

    public AceSearchFaqProperties setFields(List<String> fields) {
        this.fields = fields;
        return this;
    }
}
