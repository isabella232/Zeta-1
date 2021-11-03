package com.ebay.dss.zds.service;

import org.elasticsearch.action.index.IndexRequest;

import java.util.Optional;

public interface ElasticLogService {

    boolean add(Indexable indexable);

    void index();

    interface Indexable {
        Optional<IndexRequest> toIndexRequest();
    }

}
