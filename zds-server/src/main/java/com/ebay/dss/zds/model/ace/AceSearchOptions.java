package com.ebay.dss.zds.model.ace;

import javax.validation.constraints.NotBlank;
import java.util.Arrays;

public class AceSearchOptions {

    @NotBlank
    private String query;
    private String[] scopes = new String[0];
    private int size = 20;

    public String[] getScopes() {
        return scopes;
    }

    public AceSearchOptions setScopes(String[] scopes) {
        this.scopes = scopes;
        return this;
    }

    public String getQuery() {
        return query;
    }

    public AceSearchOptions setQuery(String query) {
        this.query = query;
        return this;
    }

    public AceSearchOptions setSize(int size) {
        this.size = size;
        return this;
    }

    @Override
    public String toString() {
        return "AceSearchOptions{" +
                "query='" + query + '\'' +
                ", scopes=" + Arrays.toString(scopes) +
                ", size=" + size +
                '}';
    }

    public int getSize() {
        return size;
    }

}
