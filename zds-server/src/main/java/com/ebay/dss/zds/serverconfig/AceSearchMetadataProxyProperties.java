package com.ebay.dss.zds.serverconfig;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "zds.ace.search.service.doe-metadata")
public class AceSearchMetadataProxyProperties {

    private String searchUri;
    private String typeUri;
    private int SearchLimit = 20;

    public String getTypeUri() {
        return typeUri;
    }

    public AceSearchMetadataProxyProperties setTypeUri(String typeUri) {
        this.typeUri = typeUri;
        return this;
    }

    public int getSearchLimit() {
        return SearchLimit;
    }

    public AceSearchMetadataProxyProperties setSearchLimit(int searchLimit) {
        this.SearchLimit = searchLimit;
        return this;
    }

    public String getSearchUri() {
        return searchUri;
    }

    public AceSearchMetadataProxyProperties setSearchUri(String searchUri) {
        this.searchUri = searchUri;
        return this;
    }

}
